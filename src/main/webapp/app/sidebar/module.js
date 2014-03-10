var module = angular.module('sidebar', ['waterfall', 'sidebarUtils']);

module.controller('sidebar', function($rootScope, $scope, endpoints, $q, stateRunner) {
  $scope.lastSync = null;

  var kill = $scope.$watch('workflow', function(val) {
    // Register workflow on $rootScope
    $rootScope.workflow = val;
    $scope.$on('$routeChangeSuccess', stateRunner($rootScope.workflow));
    kill();
  });

  endpoints.getHistory().then(function(timestamps) {
    $scope.lastSync = timestamps[0];
  });

});