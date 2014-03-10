angular.module('endpoints', []).factory('endpoints', function($http, $q) {
  var service = {};
  var setup = function () {
    var setupArgs = Array.prototype.slice.call(arguments, 0);
    return function () {
      var callArgs = arguments;
      var deferred = $q.defer();
      var i = 0;
      $http.get.call(this, setupArgs.map(function (item) {
        if (item === '$') {
          return callArgs[i++];
        } else {
          return item;
        }
      }).join('')).success(function(payload) {
        deferred.resolve(payload);
      }).error(function(payload) {
        deferred.reject(payload);
      });

      return deferred.promise;
    };
  };

  service.getHistory = setup('/history');
  service.getTerms = setup('/terms');
  service.getSchools = setup('/schools');
  service.getDepartments = setup('/departments');
  service.getCourses = setup('/courses');

  service.getSchoolById = setup('/school/', '$');
  service.getTermById = setup('/term/', '$');
  service.getSectionById = setup('/section/', '$');
  service.getDepartmentById = setup('/department/', '$');

  service.getDepartmentsBySchoolId = setup('/school/', '$', '/departments');
  service.getCoursesByDepartmentId = setup('/department/', '$', '/courses');

  return service;
});
