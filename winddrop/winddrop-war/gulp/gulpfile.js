var gulp = require('gulp');
var util = require('gulp-util');
var clean = require('gulp-clean');
var jshint = require('gulp-jshint');
var stylish = require('jshint-stylish');
var usemin = require('gulp-usemin');
var uglify = require('gulp-uglify');
var minifyCss = require('gulp-minify-css');
var w3cjs = require('gulp-w3cjs');
var htmlmin = require('gulp-htmlmin');

//var runSequence = require('run-sequence');

// # npm install --save-dev gulp gulp-util gulp-clean run-sequence gulp-requirejs bower-requirejs gulp-uglify gulp-less gulp-jshint
// npm install --save-dev gulp-usemin gulp-uglify gulp-minify-css

var destDir = '../src/main/webapp/';

gulp.task('default', ['w3cjs'], function(cb) {
	// Default task
});


// cf. https://www.npmjs.com/package/gulp-jslint for the configuration
// cf. http://jshint.com/docs/options/ for the official documentation
// cf. https://jslinterrors.com/ for an explanation about the errors
gulp.task('jshint', function() {
	return gulp.src([
		'../src/main/webapp/js/app-*.js'
	])
		.pipe(jshint({
 		   curly   : true,
    	   node    : true,
    	   indent  : 4,
           latedef : true,
    	   undef   : true,
           unused  : true,
           expr    : true,
           predef  : ['window', '$', '_', 'alert', 'document', 'XMLHttpRequest', 'FormData'],
           exported : ['getFileTemplate']
		}))
		.pipe(jshint.reporter(stylish))
		.pipe(jshint.reporter('fail'));
});

gulp.task('clean', ['jshint'], function() {
    return gulp.src([
    	'public/*',
		destDir+'index.html',
		destDir+'reports.html',
		destDir+'/js/site.js',
		destDir+'/css/site.css'
		// '../src/main/webapp/*.html'
	]).pipe(clean({force: true}));
});

gulp.task('minify', ['clean'], function() {
   return gulp.src('src/templates/*.html')
        .pipe(usemin({
            assetsDir: 'bower_components',
            css: [minifyCss(), 'concat'],
            js: [uglify(), 'concat']
        }))
        .pipe(gulp.dest(destDir));
});



gulp.task('htmlmin', ['minify'], function(){
   return gulp.src(destDir+'*.html')
  .pipe(htmlmin({collapseWhitespace: true}))
  .pipe(gulp.dest(destDir));
});


gulp.task('w3cjs', ['htmlmin'],  function () {
   return gulp.src(destDir+'*.html')
    .pipe(w3cjs());
});

// Crap plugin ...
// gulp.task('htmltidy', ['htmlmin'], function() {
//    return gulp.src(destDir+'*.html')
//         .pipe(htmltidy(
// 				{doctype: 'html5',
//                  hideComments: true,
//                  indent: true}
//         	))
//         .pipe(gulp.dest(destDir));
// });






