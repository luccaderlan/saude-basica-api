param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$DataInicio = '2026-07-27',
    [string]$DataFim = '2026-09-25',
    [long]$PacienteId = 1
)
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

function Confirmar([bool]$Condicao, [string]$Mensagem) {
    if (-not $Condicao) { throw $Mensagem }
}

function EsperarErro([string]$Caminho, [int]$Status) {
    $codigo = 0
    $corpo = $null
    try {
        Invoke-RestMethod "$BaseUrl$Caminho" | Out-Null
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $codigo = [int]$_.Exception.Response.StatusCode
        $textoErro = $_.ErrorDetails.Message
        # Windows PowerShell pode deixar ErrorDetails vazio mesmo com uma resposta JSON.
        if ([string]::IsNullOrWhiteSpace($textoErro)) {
            $respostaErro = $_.Exception.Response
            if ($respostaErro.PSObject.Methods['GetResponseStream']) {
                $leitor = New-Object System.IO.StreamReader($respostaErro.GetResponseStream())
                try {
                    $textoErro = $leitor.ReadToEnd()
                } finally {
                    $leitor.Dispose()
                }
            } elseif ($null -ne $respostaErro.Content) {
                $textoErro = $respostaErro.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            }
        }
        Confirmar (-not [string]::IsNullOrWhiteSpace($textoErro)) "HTTP $codigo sem corpo de erro em $Caminho."
        $corpo = $textoErro | ConvertFrom-Json
    }
    Confirmar ($codigo -eq $Status) "Esperava HTTP $Status em $Caminho, recebeu $codigo."
    Confirmar ($corpo.status -eq $Status) 'O erro deve conter status no JSON.'
    Write-Host "HTTP $codigo - $($corpo.mensagem)"
}

# Somente GETs: pode ser usado no banco historico da apresentacao.
$p0 = Invoke-RestMethod "$BaseUrl/api/atendimentos?page=0&size=3&sort=id,asc"
$p1 = Invoke-RestMethod "$BaseUrl/api/atendimentos?page=1&size=3&sort=id,asc"
Confirmar ($p0.number -eq 0 -and $p1.number -eq 1) 'Numero da pagina incorreto.'
Confirmar ($p0.totalElements -eq $p1.totalElements) 'Os totais das paginas diferem.'
Confirmar (@($p0.content).Count -le 3 -and @($p1.content).Count -le 3) 'Pagina excede size.'
$ids0 = @($p0.content | ForEach-Object { $_.id })
$ids1 = @($p1.content | ForEach-Object { $_.id })
Confirmar (@($ids1 | Where-Object { $ids0 -contains $_ }).Count -eq 0) 'Ha repeticao entre paginas.'
Write-Host "Pagina 0 IDs: $($ids0 -join ', '); pagina 1 IDs: $($ids1 -join ', '); total: $($p0.totalElements)"

$filtro = Invoke-RestMethod "$BaseUrl/api/atendimentos?status=CONCLUIDO&size=2"
Confirmar (@($filtro.content | Where-Object { $_.status -ne 'CONCLUIDO' }).Count -eq 0) 'Filtro incorreto.'
Write-Host "CONCLUIDO: $($filtro.totalElements) no banco; $(@($filtro.content).Count) nesta pagina"
$historico = Invoke-RestMethod "$BaseUrl/api/pacientes/$PacienteId/atendimentos?size=1"
Write-Host "Historico do paciente $PacienteId`: $($historico.totalElements), tamanho da pagina: $($historico.size)"

$relatorio = Invoke-RestMethod "$BaseUrl/api/relatorios/atendimentos?dataInicio=$DataInicio&dataFim=$DataFim"
$somaStatus = ($relatorio.porStatus.PSObject.Properties | ForEach-Object { [long]$_.Value } | Measure-Object -Sum).Sum
$somaUnidades = ($relatorio.porUnidade | Measure-Object -Property total -Sum).Sum
Confirmar ($relatorio.total -eq $somaStatus) 'Total difere da soma dos status.'
Confirmar ($relatorio.total -eq $somaUnidades) 'Total difere da soma das unidades.'
Write-Host "Relatorio de $DataInicio a $DataFim (dias inclusivos):"
$relatorio | ConvertTo-Json -Depth 6

$vazio = Invoke-RestMethod "$BaseUrl/api/relatorios/atendimentos?dataInicio=1900-01-01&dataFim=1900-01-01"
Confirmar ($vazio.total -eq 0 -and @($vazio.porUnidade).Count -eq 0) 'O periodo de controle deveria estar vazio.'
EsperarErro '/api/atendimentos?size=101' 400
EsperarErro '/api/atendimentos?status=XPTO' 400
EsperarErro '/api/atendimentos/999999999' 404
EsperarErro '/api/relatorios/atendimentos?dataInicio=2026-10-02&dataFim=2026-10-01' 400
EsperarErro '/api/relatorios/atendimentos?dataInicio=2026-02-30&dataFim=2026-03-01' 400
EsperarErro '/api/relatorios/atendimentos?dataInicio=2026-10-01' 400
$docs = Invoke-RestMethod "$BaseUrl/v3/api-docs"
Confirmar ($null -ne $docs.paths.'/api/relatorios/atendimentos') 'Relatorio ausente no OpenAPI.'
Write-Host 'Demonstracao concluida: paginacao, relatorio, erros e OpenAPI verificados apenas por leitura.'
