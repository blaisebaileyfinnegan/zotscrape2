var module = angular.module('departments', []);

module.controller('departments', function($rootScope, $scope, endpoints, $routeParams) {
  $scope.departments = null;
  endpoints.getDepartmentsBySchoolId($routeParams.schoolId).then(function(departments) {
    $scope.departments = departments;
  });
});