var module = angular.module('schools', []);

module.controller('schools', function($rootScope, $scope, endpoints) {
  $scope.schools = null;
  endpoints.getSchools().then(function(schools) {
    $scope.schools = schools;
  });
});