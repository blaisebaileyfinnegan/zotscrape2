module.exports = function(grunt) {
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    clean: ['<%= pkg.src %>/dist'],
    compass: {
      dev: {
        options: {
          sassDir: '<%= pkg.src %>/stylesheets',
          cssDir: '<%= pkg.dist %>/stylesheets'
        }
      }
    },
    copy: {
      buildDev: {
        files: [
          {
            expand: true,
            cwd: '<%= pkg.src %>',
            src: ['index.html', 'app/**', 'components/**', 'partials/**'],
            dest: '<%= pkg.dist %>'
          }
        ]
      }
    },
    karma: {
      unit: {
        options: {
          files: [
            '<%= pkg.src %>/components/lodash/dist/lodash.js',
            '<%= pkg.src %>/components/angular/angular.js',
            '<%= pkg.src %>/components/angular-route/angular-route.js',
            '<%= pkg.src %>/components/angular-animate/angular-animate.js',
            '<%= pkg.src %>/components/angular-strap/dist/angular-strap.js',
            '<%= pkg.src %>/components/angular-strap/dist/angular-strap.tpl.js',
            '<%= pkg.src %>/app/**/*.js',
            '<%= pkg.src %>/components/angular-mocks/angular-mocks.js',
            '<%= pkg.src %>/test/unit/**/*.js'
          ],
          frameworks: ['jasmine'],
          runnerPort: 9876,
          browsers: ['PhantomJS', 'Chrome', 'Firefox'],
          reporters: 'dots',
          background: true
        }
      }
    },
    watch: {
      src: {
        files: [
          'index.html',
          'app/**',
          'partials/**',
          'stylesheets/**'
        ],
        tasks: ['clean', 'compass:dev', 'copy:buildDev', 'karma:unit:run'],
        options: {
          cwd: '<%= pkg.src %>'
        }
      },
      karma: {
        files: [
          'test/unit/**'
        ],
        tasks: ['karma:unit:run'],
        options: {
          cwd: '<%= pkg.src %>'
        }
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-compass');
  grunt.loadNpmTasks('grunt-processhtml');
  grunt.loadNpmTasks('grunt-ngmin');
  grunt.loadNpmTasks('grunt-karma');

  grunt.registerTask('default', ['clean', 'compass:dev', 'copy:buildDev']);
  grunt.registerTask('karma-watch', ['karma:unit:start', 'watch']);
};
