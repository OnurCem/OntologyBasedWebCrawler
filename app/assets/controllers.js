var app = angular.module("OntMngApp", ["xeditable"]);

app.run(function(editableOptions) {
    editableOptions.theme = 'bs3'; // bootstrap3 theme
});

app.controller("SearchCtrl", function($scope, $filter, $http) {
    $scope.isShowComments = false;

    // Search
    $scope.search = function() {
        $scope.isShowComments = false;
        $http.get('/search?q=' + $scope.keywords).
            success(function(data, status, headers, config) {
                $scope.result = data;
            }).
            error(function(data, status, headers, config) {
                alert(status);
            });
    };

    // Get comments
    $scope.getComments = function(productId, brand, model) {
        $http.get('/getComments?q=' + productId).
            success(function(data, status, headers, config) {
                $scope.isShowComments = true;
                $scope.productName = brand + ' ' + model;
                $scope.comments = data;
            }).
            error(function(data, status, headers, config) {
                alert(status);
            });
    };

    // Calculates rating percentage
    $scope.calculateRating = function(rating) {
        var percentage = Math.round((rating/5)*100);
        $scope.rating = percentage + '%';
        return true;
    };
});

app.controller("EditableRowCtrl", function($scope, $filter, $http) {
    $scope.selectedTab = 1;

    // Get groups
    $http.get('/getGroups').
        success(function(data, status, headers, config) {
            $scope.groups = data;
        }).
        error(function(data, status, headers, config) {
            alert(status);
        });

    // Save group
    $scope.saveGroup = function(data, id) {
        angular.extend(data, {id: id});
        return $http.post('/saveGroup', data);
    };

    // Remove group
    $scope.removeGroup = function(index) {
        var data = $scope.groups[index];
        $scope.groups.splice(index, 1);
        return $http.post('/removeGroup', data);
    };

    // Add new group field to table
    $scope.addGroup = function() {
        $scope.inserted = {
            id: "group_" + Date.now(),
            name: ''
        };
        $scope.groups.push($scope.inserted);
    };

    // Get products
    $http.get('/getProducts').
        success(function(data, status, headers, config) {
            $scope.products = data;
        }).
        error(function(data, status, headers, config) {
            alert(status);
        });

    $scope.loadGroups = function() {
        return $scope.products.length ? null : $http.get('/getGroups').success(function(data) {
            $scope.groups = data;
        });
    };

    $scope.showGroup = function(p) {
        var selected = [];
        if (p.id) {
            selected = $filter('filter')($scope.groups, {id: p.groupId});
        }
        return selected.length ? selected[0].name : 'Not set';
    };

    // Save product
    $scope.saveProduct = function(data, id) {
        angular.extend(data, {id: id});
        return $http.post('/saveProduct', data);
    };

    // Add new product field to table
    $scope.addProduct = function() {
        $scope.inserted = {
            id: "product_" + Date.now(),
            brand: '',
            model: '',
            groupId: ''
        };
        $scope.products.push($scope.inserted);
    };

    // Get properties
    $http.get('/getProperties').
        success(function(data, status, headers, config) {
            $scope.properties = data;
        }).
        error(function(data, status, headers, config) {
            alert(status);
        });

    // Save property
    $scope.saveProperty = function(data, id) {
        angular.extend(data, {id: id});
        return $http.post('/saveProperty', data);
    };

    // Add new property field to table
    $scope.addProperty = function() {
        $scope.inserted = {
            id: "property_" + Date.now(),
            name: ''
        };
        $scope.properties.push($scope.inserted);
    };

    // Get product-properties
    $http.get('/getProductProperties').
        success(function(data, status, headers, config) {
            $scope.productProperties = data;
        }).
        error(function(data, status, headers, config) {
            alert(status);
        });

    $scope.loadProducts = function() {
        return $scope.products.length ? null : $http.get('/getProducts').success(function(data) {
            $scope.products = data;
        });
    };

    $scope.loadProperties = function() {
        return $scope.properties.length ? null : $http.get('/getProperties').success(function(data) {
            $scope.properties = data;
        });
    };

    $scope.showProduct = function(p) {
        var selected = [];
        if (p.productId) {
            selected = $filter('filter')($scope.products, {id: p.productId});
        }
        return selected.length ? selected[0].model : 'Not set';
    };

    $scope.showProperty = function(p) {
        var selected = [];
        if (p.propertyId) {
            selected = $filter('filter')($scope.properties, {id: p.propertyId});
        }
        return selected.length ? selected[0].name : 'Not set';
    };

    // Add new product-property field to table
    $scope.addProductProperty = function() {
        $scope.inserted = {
            productId: '',
            propertyId: ''
        };
        $scope.productProperties.push($scope.inserted);
    };

    // Save product-property
    $scope.saveProductProperty = function(data) {
        return $http.post('/saveProductProperty', data);
    };
});