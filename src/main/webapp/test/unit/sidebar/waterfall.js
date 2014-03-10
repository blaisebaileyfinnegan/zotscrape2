describe('waterfall directive', function() {
  var scope;

  beforeEach(module('waterfall'));

  describe('with some stuff in it', function() {
    beforeEach(inject(function($rootScope, $compile) {
      var element =
        '<div v-waterfall="test">' +
          '<div v-waterfall-step="one"></div>' +
          '<div v-waterfall-step="two"></div>' +
          '<div v-waterfall-step="three"></div>' +
          '<div v-waterfall-step="four"></div>' +
        '</div>';

      scope = $rootScope.$new();
      $compile(element)(scope);
      scope.$digest();
    }));

    it('should have properties added', function() {
      expect(scope.test.steps).toEqual({
        one: false,
        two: false,
        three: false,
        four: false
      });

      expect(scope.test.stepsOrdering).toEqual([
        'one',
        'two',
        'three',
        'four'
      ]);
    });

    it('should start at 0', function() {
      expect(scope.test.currentStepIndex).toBe(0);
      expect(scope.test.currentStep).toBe('one');
    });

    it('should waterfall to steps.length', function() {
      scope.test.stepTo(50);
      expect(scope.test.currentStepIndex).toBe(scope.test.stepsOrdering.length);
      expect(scope.test.currentStep).toBe(null);

      scope.test.stepTo(0);
      expect(scope.test.currentStepIndex).toBe(0);
      expect(scope.test.currentStep).toBe('one');

      scope.test.stepTo('three');
      expect(scope.test.currentStepIndex).toBe(2);
      expect(scope.test.currentStep).toBe('three');
    });
  })
});