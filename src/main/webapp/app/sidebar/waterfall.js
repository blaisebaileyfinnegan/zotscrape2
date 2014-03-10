var module = angular.module('waterfall', []);

module.directive('vWaterfall', function() {
  return {
    restrict: 'A',
    controller: function($scope, $element, $attrs) {
      var name = $attrs.vWaterfall;

      var obj = this.$waterfall = $scope[name] = {};

      obj.currentStepIndex = -1;
      obj.currentStep = null;
      obj.steps = {};
      obj.stepsOrdering = [];

      this.reset = obj.reset = function() {
        obj.stepTo(0);
      };

      this.nextStep = obj.nextStep = function(info) {
        if (obj.currentStepIndex < obj.stepsOrdering.length) {
          obj.steps[obj.currentStep] = info;
          obj.currentStepIndex++;

          if (obj.currentStepIndex == obj.stepsOrdering.length) {
            obj.currentStep = null;
          } else {
            obj.currentStep = obj.stepsOrdering[obj.currentStepIndex];
          }

          return true;
        } else {
          return false;
        }
      };

      this.previousStep = obj.previousStep = function() {
        if (obj.currentStepIndex > 0) {
          obj.currentStepIndex--;
          var currentStep = obj.currentStep = obj.stepsOrdering[obj.currentStepIndex];
          obj.steps[currentStep] = false;

          return true;
        } else {
          return false;
        }
      };

      this.stepTo = obj.stepTo = function(target, info) {
        var distance = 0;
        if (typeof target === "number") {
          distance = target - obj.currentStepIndex;
        } else {
          var targetStepIndex = obj.stepsOrdering.indexOf(target);
          distance = targetStepIndex - obj.currentStepIndex;
        }

        for (; distance != 0;) {
          if (distance < 0) {
            obj.previousStep();
            distance++;
          } else {
            var payload;
            if (info) {
              payload = info[obj.currentStep];
            }

            obj.nextStep(payload);
            distance--;
          }
        }
      };
    }
  };
});

module.directive('vWaterfallStep', function() {
  return {
    restrict: 'A',
    require: '^vWaterfall',
    link: function(scope, elem, attrs, parentController) {
      var name = attrs.vWaterfallStep;

      parentController.$waterfall.steps[name] = false;
      parentController.$waterfall.stepsOrdering.push(name);

      if (parentController.$waterfall.currentStepIndex < 0) {
        var i = parentController.$waterfall.currentStepIndex = 0;
        parentController.$waterfall.currentStep = parentController.$waterfall.stepsOrdering[i];
      }
    }
  };
});