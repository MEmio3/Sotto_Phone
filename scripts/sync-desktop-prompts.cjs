// Maintenance helper. The APK uses the bundled JSON and needs no desktop install.
const fs = require('node:fs');
const path = require('node:path');
const source = path.resolve(process.argv[2] || path.join(__dirname, '..', '..', 'Sotto'));
const { DEFAULT_SETTINGS, WRITING_MODES, buildInstructions } = require(path.join(source, 'electron', 'core.cjs'));
const rules = {};
for (const [id, mode] of Object.entries(WRITING_MODES)) {
  let template = buildInstructions({ ...DEFAULT_SETTINGS, writingMode: id, outputLanguage: '{{outputLanguage}}' }, '{{override}}');
  template = template.replace(/^The user explicitly selected.*$/m, '{{languageInstructions}}');
  template = template.replace(/^Remove clearly unrelated conversational asides.*$/m, '{{cleanupInstructions}}');
  rules[id] = { label: mode.label, description: mode.description, template };
}
fs.writeFileSync(path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'dictation-rules.json'), JSON.stringify(rules, null, 2));
process.stdout.write('Desktop writing-mode prompts synchronized.\n');
