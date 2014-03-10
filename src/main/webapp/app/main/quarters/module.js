var module = angular.module('quarters', []);

module.controller('quarters', function($scope, endpoints) {
  $scope.terms = null;
  endpoints.getTerms().then(function(terms) {
    $scope.terms = terms;
  });
});