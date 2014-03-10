var vendor = ['ngRoute', 'ngAnimate', 'mgcrea.ngStrap'];
var modules = ['endpoints', 'sidebar', 'quarters', 'schools', 'departments', 'courses'];

var module = angular.module('antbutter', vendor.concat(modules));

module.config(function($routeProvider) {
  $routeProvider.when('/', {
    templateUrl: 'partials/main/quarters/list.html',
    controller: 'quarters'
  }).when('/:termId', {
    templateUrl: 'partials/main/schools/list.html',
    controller: 'schools'
  }).when('/:termId/:schoolId', {
    templateUrl: 'partials/main/departments/list.html',
    controller: 'departments'
  }).when('/:termId/:schoolId/:departmentId', {
    templateUrl: 'partials/main/courses/list.html',
    controller: 'courses'
  }).when('/:termId/:schoolId/:departmentId/:courseId', {
    templateUrl: 'partials/main/sections/list.html',
    controller: 'sections'
  }).otherwise({
    redirectTo: '/'
  })
});
