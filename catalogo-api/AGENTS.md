# Instruções do projeto

Leia integralmente [trabalho-final.md](trabalho-final.md) antes de propor ou implementar mudanças. Esse é o enunciado principal; as decisões explícitas do usuário complementam o domínio.

- API de agendamento de massagem por sala, data e período inteiro (MANHA, TARDE, NOITE).
- Java 21, Quarkus, Maven, H2 e mock HTTP local de feriados por UF/data.
- Manter Package by Feature + Boundary / Domain / Repository e representações separadas: Entity para JPA, BO para negócio e DTOs para HTTP.
- Resource recebe RequestDto, passa campos ao Service e converte BO com ResponseDto.from. Service não depende de DTO HTTP.
- Regras e transações no Service CDI; persistência no PanacheRepository. Lombok pode gerar construtores, sem @Data indiscriminado em entidade.
- Sem frontend, autenticação ou CRUD de salas/usuários nesta versão. IDs de sala são globais.
- Relacionar mudanças aos requisitos 01–16 e atualizar docs/evidencias.md. Diferenciar implementação, testes executados e demonstrações pendentes.
- Preservar alterações do usuário. Não apagar arquivos de dados nem recriar banco persistente para resolver problemas.
- Rodar testes com JDK 21. Nunca gravar credenciais no repositório.
