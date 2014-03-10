var module = angular.module('courses', []);

module.controller('courses', function($rootScope, $scope, endpoints, $routeParams) {
  $scope.courses = null;
  endpoints.getCoursesByDepartmentId($routeParams.departmentId).then(function(courses) {
    $scope.courses = courses;
  });
});