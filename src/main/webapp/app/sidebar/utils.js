var module = angular.module('sidebarUtils', ['endpoints']);

module.factory('stateRunner', function(endpoints, $q) {
  return function(state) {
    return function(event, route) {
      var jumpTo = function(destination, arr) {
        var choices = {
          term: function() {
            return endpoints.getTermById(route.params.termId);
          },
          school: function() {
            return endpoints.getSchoolById(route.params.schoolId);
          },
          department: function() {
            return endpoints.getDepartmentById(route.params.departmentId);
          }
        };

        var payload = {};
        arr.forEach(function(param) {
          payload[param] = choices[param]();
        });

        $q.all(payload).then(function(info) {
          state.stepTo(destination, info);
        })
      };

      if (route.originalPath === '/') {
        state.reset();
      } else if (route.originalPath === '/:termId') {
        jumpTo('school', ['term']);
      } else if (route.originalPath === '/:termId/:schoolId') {
        jumpTo('department', ['term', 'school']);
      } else if (route.originalPath === '/:termId/:schoolId/:departmentId') {
        jumpTo('course', ['term', 'school', 'department']);
      }
    };
  };
});