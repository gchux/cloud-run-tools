import { series } from 'gulp';
import { $ } from 'zx'

function clean(cb) {
  cb();
}

function build(cb) {
  $`npm run build`
  cb();
}

exports.build = build;
exports.default = series(clean, build);