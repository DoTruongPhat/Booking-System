// Test script: Lấy public key từ BE, encrypt bằng PowerShell (.NET), gửi Java decrypt
const http = require('http');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

async function getPublicKey() {
  return new Promise((resolve, reject) => {
    http.get('http://localhost:8081/api/auth/public-key', (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(JSON.parse(data)));
    }).on('error', reject);
  });
}

const psScript = `
param([string]$pem, [string]$outFile)

$PemContent = @"
$($pem -replace '-----BEGIN PUBLIC KEY-----', '' -replace '-----END PUBLIC KEY-----', '' -replace '\\s', '')
"@

# Strip BEGIN/END
$body = $pem -replace '-----BEGIN PUBLIC KEY-----', '' `
            -replace '-----END PUBLIC KEY-----', '' `
            -replace '\\s', ''

$der = [Convert]::FromBase64String($body)
$rsa = [System.Security.Cryptography.RSA]::Create()
$rsa.ImportSubjectPublicKeyInfo($der, [ref]0)

# OAEP SHA-256 with MGF1 SHA-1 (matches Web Crypto spec)
$plain = [System.Text.Encoding]::UTF8.GetBytes('test123')
$encrypted = $rsa.Encrypt($plain, [System.Security.Cryptography.RSAEncryptionPadding]::OaepSHA256)
[System.IO.File]::WriteAllBytes($outFile, $encrypted)
Write-Host "PowerShell encrypt OK, length: $($encrypted.Length)"
`;

(async () => {
  try {
    const info = await getPublicKey();
    console.log('Got PEM length:', info.publicKey.length);

    const tmpDir = process.env.TEMP || 'C:\\Users\\tphat\\AppData\\Local\\Temp';
    const cipherFile = path.join(tmpDir, 'test-cipher.bin');
    const psFile = path.join(tmpDir, 'test-encrypt.ps1');

    fs.writeFileSync(psFile, psScript);

    // Run PowerShell với PEM làm argument
    const psCommand = `powershell -ExecutionPolicy Bypass -File "${psFile}" -pem "${info.publicKey.replace(/\n/g, '`n')}" -outFile "${cipherFile}"`;

    console.log('Running PowerShell...');
    const out = execSync(psCommand, { encoding: 'utf8', shell: true });
    console.log(out);

    const cipherBytes = fs.readFileSync(cipherFile);
    const b64Standard = cipherBytes.toString('base64');
    const b64UrlSafe = cipherBytes.toString('base64url');

    console.log('Cipher length:', cipherBytes.length, 'bytes');
    console.log('Standard b64 length:', b64Standard.length);
    console.log('URL-safe b64 length:', b64UrlSafe.length);
    console.log('Has + or /:', /[+/=]/.test(b64Standard));
    console.log('Has - or _:', /[-_]/.test(b64UrlSafe));
    console.log('Standard b64:', b64Standard);
    console.log('URL-safe b64:', b64UrlSafe);

    // Gửi cho Java decrypt
    const postData = JSON.stringify({ ciphertext: b64Standard });
    const req = http.request({
      hostname: 'localhost',
      port: 8081,
      path: '/api/auth/debug/decrypt',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => console.log('\n>>> Java decrypt result:', body));
    });
    req.write(postData);
    req.end();
  } catch (e) {
    console.error('Error:', e.message);
    if (e.stderr) console.error('stderr:', e.stderr.toString());
  }
})();
