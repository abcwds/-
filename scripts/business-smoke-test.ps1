param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
)

$ErrorActionPreference = 'Stop'

$script:passed = 0
$script:failed = 0
$script:httpClient = [System.Net.Http.HttpClient]::new()

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [string]$Token,
        [object]$Body,
        [int]$ExpectedStatus = 200,
        [int]$ExpectedCode = 200
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$BaseUrl$Path"
    )
    if ($Token) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
    }
    if ($null -ne $Body) {
        $request.Content = [System.Net.Http.StringContent]::new(
            ($Body | ConvertTo-Json -Depth 8),
            [System.Text.Encoding]::UTF8,
            'application/json'
        )
    }

    try {
        $response = $script:httpClient.SendAsync($request).GetAwaiter().GetResult()
        $status = [int]$response.StatusCode
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $json = $content | ConvertFrom-Json
    } finally {
        $request.Dispose()
        if ($null -ne $response) {
            $response.Dispose()
        }
    }

    if ($status -ne $ExpectedStatus) {
        throw "HTTP $status, expected $ExpectedStatus, path=$Path"
    }
    if ($null -ne $ExpectedCode -and [int]$json.code -ne $ExpectedCode) {
        throw "code $($json.code), expected $ExpectedCode, path=$Path, message=$($json.message)"
    }
    return $json
}

function Assert-Case {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    try {
        & $Action | Out-Null
        $script:passed++
        Write-Host "[PASS] $Name" -ForegroundColor Green
    } catch {
        $script:failed++
        Write-Host "[FAIL] $Name :: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function New-TestUser {
    param([string]$Prefix, [string]$PhonePrefix)
    $stamp = Get-Date -Format 'MMddHHmmss'
    $phoneSuffix = $stamp.Substring(2)
    $username = "$Prefix$stamp"
    $phone = "$PhonePrefix$phoneSuffix"
    $body = @{
        username = $username
        password = 'Test123456'
        nickname = "流程测试-$stamp"
        phone = $phone
        email = "$username@qa.pet.local"
    }
    $result = Invoke-Api -Method 'POST' -Path '/api/auth/register' -Body $body
    return @{
        Username = $username
        Password = 'Test123456'
        Token = $result.data.token
    }
}

Write-Host "Pet adoption business smoke test: $BaseUrl" -ForegroundColor Cyan

Assert-Case 'TC-01 service health' {
    $health = Invoke-Api -Method 'GET' -Path '/api/system/health'
    if ($health.data.status -ne 'UP' -or $health.data.database -ne 'UP') {
        throw "service or database is not UP"
    }
}

Assert-Case 'TC-02 root endpoint is public' {
    $root = Invoke-Api -Method 'GET' -Path '/'
    if ($root.data.service -ne 'pet-adoption') {
        throw 'unexpected service name'
    }
}

$adminLogin = Invoke-Api -Method 'POST' -Path '/api/auth/login' -Body @{
    username = 'admin'
    password = '123456'
}
$adminToken = $adminLogin.data.token

Assert-Case 'TC-03 administrator login' {
    if ($adminLogin.data.role -ne 1 -or [string]::IsNullOrWhiteSpace($adminToken)) {
        throw 'administrator token or role is missing'
    }
}

$userLogin = Invoke-Api -Method 'POST' -Path '/api/auth/login' -Body @{
    username = 'user1'
    password = '123456'
}
$userToken = $userLogin.data.token

Assert-Case 'TC-04 unauthenticated request is rejected' {
    Invoke-Api -Method 'GET' -Path '/api/auth/me' -ExpectedStatus 401 -ExpectedCode 401
}

Assert-Case 'TC-05 ordinary user cannot access admin API' {
    Invoke-Api -Method 'GET' -Path '/api/admin/users?page=1&size=1' -Token $userToken `
        -ExpectedStatus 403 -ExpectedCode 403
}

$pets = Invoke-Api -Method 'GET' -Path '/api/pets?page=1&size=20'
$availablePets = @($pets.data.records | Where-Object { $_.status -eq 0 })
if ($availablePets.Count -lt 2) {
    throw 'at least two available pets are required for the workflow test'
}
$petA = $availablePets[0]
$petB = $availablePets[1]

Assert-Case 'TC-06 browse pet health and vaccine records' {
    $detail = Invoke-Api -Method 'GET' -Path "/api/pets/$($petA.id)"
    $health = Invoke-Api -Method 'GET' -Path "/api/health/pet/$($petA.id)"
    $vaccine = Invoke-Api -Method 'GET' -Path "/api/vaccine/pet/$($petA.id)"
    if ($detail.data.id -ne $petA.id -or $null -eq $health.data -or $null -eq $vaccine.data) {
        throw 'pet detail or records are missing'
    }
}

Assert-Case 'TC-07 data sources are traceable' {
    $sources = Invoke-Api -Method 'GET' -Path '/api/system/data-sources'
    if (@($sources.data).Count -lt 3) {
        throw 'registered source list is too short'
    }
}

$qaUserA = New-TestUser -Prefix 'qaA' -PhonePrefix '139'
$qaUserB = New-TestUser -Prefix 'qaB' -PhonePrefix '138'

$applicationA = Invoke-Api -Method 'POST' -Path '/api/adoptions' -Token $qaUserA.Token -Body @{
    petId = $petA.id
    realName = '流程测试甲'
    phone = '13912345678'
    address = '上海市浦东新区世纪大道100号'
    workStatus = '全职工作，日常作息稳定'
    housingType = '自有住宅'
    hasOtherPet = 0
    experience = '有三年科学养宠经验'
    reason = '希望为宠物提供长期稳定的家庭环境'
}
$applicationAId = $applicationA.data.id

Assert-Case 'TC-08 submit complete adoption application' {
    if ($applicationA.data.status -ne 0) {
        throw 'new application is not pending'
    }
}

Assert-Case 'TC-09 duplicate application is rejected' {
    Invoke-Api -Method 'POST' -Path '/api/adoptions' -Token $qaUserA.Token -Body @{
        petId = $petA.id
        realName = '流程测试甲'
        phone = '13912345678'
        address = '上海市浦东新区世纪大道100号'
        reason = '重复提交检查'
    } -ExpectedStatus 200 -ExpectedCode 400
}

$applicationB = Invoke-Api -Method 'POST' -Path '/api/adoptions' -Token $qaUserB.Token -Body @{
    petId = $petA.id
    realName = '流程测试乙'
    phone = '13812345678'
    address = '北京市朝阳区建国路88号'
    workStatus = '自由职业'
    housingType = '租住住宅，已获房东同意'
    hasOtherPet = 1
    experience = '有猫咪照护经验'
    reason = '家庭成员已达成一致，具备持续照护时间'
}
$applicationBId = $applicationB.data.id

$applicationForReject = Invoke-Api -Method 'POST' -Path '/api/adoptions' -Token $qaUserA.Token -Body @{
    petId = $petB.id
    realName = '流程测试甲'
    phone = '13912345678'
    address = '上海市浦东新区世纪大道100号'
    workStatus = '全职工作'
    housingType = '自有住宅'
    hasOtherPet = 0
    experience = '有养宠经验'
    reason = '拒绝理由校验流程'
}
$rejectId = $applicationForReject.data.id

Assert-Case 'TC-10 rejection requires a reason' {
    Invoke-Api -Method 'PUT' -Path "/api/admin/adoptions/$rejectId/review" -Token $adminToken `
        -Body @{ status = 2 } -ExpectedStatus 200 -ExpectedCode 400
}

Assert-Case 'TC-11 reject application with an auditable reason' {
    Invoke-Api -Method 'PUT' -Path "/api/admin/adoptions/$rejectId/review" -Token $adminToken `
        -Body @{ status = 2; rejectReason = '当前家庭环境暂不满足该宠物的照护需求' }
}

Assert-Case 'TC-12 approve application and close competing applications' {
    Invoke-Api -Method 'PUT' -Path "/api/admin/adoptions/$applicationAId/review" -Token $adminToken `
        -Body @{ status = 1 }
    $competing = Invoke-Api -Method 'GET' -Path "/api/admin/adoptions/$applicationBId" -Token $adminToken
    if ($competing.data.status -ne 2) {
        throw 'competing application was not automatically rejected'
    }
}

Assert-Case 'TC-13 adopted pet cannot be applied for again' {
    Invoke-Api -Method 'POST' -Path '/api/adoptions' -Token $qaUserB.Token -Body @{
        petId = $petA.id
        realName = '流程测试乙'
        phone = '13812345678'
        address = '北京市朝阳区建国路88号'
        reason = '已领养宠物保护检查'
    } -ExpectedStatus 200 -ExpectedCode 400
}

Write-Host ""
Write-Host "Passed: $script:passed, Failed: $script:failed" -ForegroundColor Cyan
if ($script:failed -gt 0) {
    exit 1
}
