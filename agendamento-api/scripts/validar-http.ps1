param(
    [string]$BaseUrl = 'http://localhost:9091',
    [switch]$FalhaExterna
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(20)
function Requisicao([string]$Metodo, [string]$Rota, [int]$Esperado, [string]$Json = '') {
    $mensagem = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Metodo), "$BaseUrl$Rota")
    if ($Json) { $mensagem.Content = [System.Net.Http.StringContent]::new($Json, [Text.Encoding]::UTF8, 'application/json') }
    $resposta = $client.SendAsync($mensagem).GetAwaiter().GetResult()
    $texto = $resposta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$resposta.StatusCode -ne $Esperado) {
        throw "$Metodo $Rota esperado=$Esperado recebido=$([int]$resposta.StatusCode): $texto"
    }
    if ($Esperado -ge 400) {
        $erro = $texto | ConvertFrom-Json
        foreach ($campo in @('codigo','mensagem','timestamp','path','detalhes')) {
            if ($erro.PSObject.Properties.Name -notcontains $campo) { throw "Erro sem campo $campo" }
        }
    }
    Write-Host "OK $Metodo $Rota -> $Esperado"
    [PSCustomObject]@{ Texto = $texto; Location = $resposta.Headers.Location }
    $resposta.Dispose()
    $mensagem.Dispose()
}
function Contador {
    $r = Requisicao GET '/q/metrics' 200
    if ($r.Texto -notmatch '(?m)^agendamentos_criados_total\s+([\d.eE+-]+)') { throw 'Contador ausente' }
    [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
}
$ano = (Get-Date).Year + 1
$sala = Get-Random -Minimum 100000 -Maximum 1000000000
$dados = @{id_sala=$sala; id_usuario=10; uf='SP'; data="$ano-02-10"; periodo='MANHA'}
$id = $null
try {
    if ($FalhaExterna) {
        $antes = Contador
        $null = Requisicao POST '/agendamentos' 503 ($dados | ConvertTo-Json)
        if ((Contador) -ne $antes) { throw 'Falha externa incrementou contador' }
        $lista = (Requisicao GET '/agendamentos' 200).Texto | ConvertFrom-Json
        if (@($lista | Where-Object id_sala -eq $sala).Count -ne 0) { throw 'Falha externa gravou reserva' }
        Write-Host 'Cenário de indisponibilidade verificado.'
        return
    }
    $null = Requisicao GET '/info' 200
    $null = Requisicao GET '/q/health/live' 200
    $null = Requisicao GET '/q/health/ready' 200
    $antes = Contador
    $criada = Requisicao POST '/agendamentos' 201 ($dados | ConvertTo-Json)
    $id = ($criada.Texto | ConvertFrom-Json).id
    if ($criada.Location -notmatch "/agendamentos/$id$") { throw 'Location incorreto' }
    if ((Contador) -ne ($antes + 1)) { throw 'Métrica não aumentou em um' }
    $null = Requisicao GET '/agendamentos' 200
    $null = Requisicao GET "/agendamentos/$id" 200
    $null = Requisicao POST '/agendamentos' 409 ($dados | ConvertTo-Json)
    $null = Requisicao PUT "/agendamentos/$id" 200 ($dados | ConvertTo-Json)
    $dados.periodo = 'TARDE'
    $null = Requisicao PUT "/agendamentos/$id" 200 ($dados | ConvertTo-Json)
    $atualizada = (Requisicao GET "/agendamentos/$id" 200).Texto | ConvertFrom-Json
    if ($atualizada.periodo -ne 'TARDE') { throw 'PUT não persistiu' }
    $null = Requisicao POST '/agendamentos' 400 '{}'
    $null = Requisicao POST '/agendamentos' 400 '{'
    $dados.data = "$ano-01-01"
    $null = Requisicao POST '/agendamentos' 409 ($dados | ConvertTo-Json)
    if ((Contador) -ne ($antes + 1)) { throw 'Atualização ou falha incrementou contador' }
    $null = Requisicao DELETE "/agendamentos/$id" 204
    $null = Requisicao GET "/agendamentos/$id" 404
    $null = Requisicao DELETE "/agendamentos/$id" 404
    $id = $null
    Write-Host 'Fluxo HTTP completo verificado.'
} finally {
    if ($null -ne $id) {
        try { $null = Requisicao DELETE "/agendamentos/$id" 204 } catch { Write-Warning $_ }
    }
    $client.Dispose()
}
