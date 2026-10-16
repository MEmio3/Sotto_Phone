// Keep this key and its password files private so future APKs can update this app.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');
const root = path.join(__dirname, '..');
const properties = path.join(root, 'signing.properties');
const key = path.join(root, '.signing', 'sotto-release.jks');
if (fs.existsSync(properties) && fs.existsSync(key)) process.exit(0);
if (fs.existsSync(properties) || fs.existsSync(key)) throw new Error('Signing files are incomplete. Restore the matching private key and signing.properties before building an update.');
fs.mkdirSync(path.dirname(key), { recursive: true });
const password = crypto.randomBytes(32).toString('base64url');
const tool = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'keytool.exe') : 'keytool';
const result = spawnSync(tool, ['-genkeypair', '-keystore', key, '-storetype', 'JKS', '-storepass:env', 'SOTTO_SIGNING_PASSWORD', '-keypass:env', 'SOTTO_SIGNING_PASSWORD', '-alias', 'sotto', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000', '-dname', 'CN=MEmio3, OU=Sotto, O=Sotto, L=Dhaka, C=BD'], { env: { ...process.env, SOTTO_SIGNING_PASSWORD: password }, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true });
if (result.status !== 0) throw new Error('Could not generate the local Android signing key. Check your JDK installation.');
fs.writeFileSync(properties, `storeFile=.signing/sotto-release.jks\nstorePassword=${password}\nkeyAlias=sotto\nkeyPassword=${password}\n`);
process.stdout.write('Private signing key prepared locally.\n');
