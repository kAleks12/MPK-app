package com.example.app;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class App extends Application implements EventHandler<javafx.event.ActionEvent> {
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private MapView mapView;
    private Polygon surveillanceArea = null;

    private StackPane root;

    private final GraphicsOverlay overlay = new GraphicsOverlay();

    LinkedList <String> surveillancedLines = new LinkedList<>();
    private final LinkedList<Point> referencePoints = new LinkedList<>();

    private final LinkedList<String> activeLines = new LinkedList<>();
    private final LinkedList<Vehicle> vehiclesToPrint = new LinkedList<>();


    public String[] lines = {
            "A", "C", "D", "K", "N", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111",
            "112", "113", "114", "115", "116", "118", "119", "120", "121", "122", "124", "125", "126", "127", "128",
            "129", "130", "131", "132", "133", "134", "136", "140", "142", "143", "144", "145", "146", "147", "148",
            "149", "150", "151", "206", "240", "241", "242", "243", "244", "245", "246", "247", "248", "249", "250",
            "251", "253", "255", "257", "259", "315", "319", "602", "607", "700", "703", "731", "1", "2", "3", "4",
            "5", "6", "7", "8", "9", "10", "11", "15", "16", "17", "20", "23", "31", "33", "70", "74"
    };
    private long refreshInterval = 5;
    private long areaRadius = 5;


    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage window) {
        Image appIcon = new Image("https://www.wroclaw.pl/files/cmsdocuments/58059480/autobusy-aglomeracja-wroclawska.jpg");
        window.getIcons().add(appIcon);

        // set the title and size of the stage and show it
        window.setTitle("mpk surveillance");
        window.setWidth(1400);
        window.setHeight(960);
        window.show();
        window.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        // create a JavaFX scene with a stack pane as the root node, and add it to the scene
        root = new StackPane();
        Scene scene = new Scene(root);

        window.setScene(scene);

        String yourApiKey = "AAPK2d91d6ab3c5d44cc9077f5ad28c799ee0NteptnnCqq7aQHQ4axkxVCLAvPOxbDKhzS8uB-hr3C_uCcwa-qvpQpVrG4YR5AY";
        ArcGISRuntimeEnvironment.setApiKey(yourApiKey);

        mapView = new MapView();
        mapView.setOnMouseClicked(mapMouseHandler);
        root.getChildren().add(mapView);

        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_NAVIGATION);
        mapView.setMap(map);
        map.setMaxScale(4000);
        map.setMinScale(110000);

        mapView.setViewpoint(new Viewpoint(51.109405517578, 17.047897338867, 110000));

        mapView.getGraphicsOverlays().add(overlay);


        createGUI();
        refreshElements();
    }

    @Override
    public void stop() {
        if (mapView != null)
            mapView.dispose();
    }

    /*
        ///////////////////////////
         Creating GUI over the map
        ///////////////////////////
    */

    public void createGUI() {
        VBox GUI = new VBox();
        root.getChildren().add(GUI);
        StackPane.setAlignment(GUI, Pos.TOP_LEFT);
        GUI.setSpacing(5);
        GUI.setPadding(new Insets(6, 6, 10, 5));
        GUI.setMaxSize(root.getWidth() / 8.5, root.getHeight() / 2);
        GUI.setStyle("""
                        -fx-background-radius: 14;
                        -fx-background-color: rgb(189,174,174);
                """);

        createButtons(GUI);
        createRefreshSegment(GUI);
        createAreaSegment(GUI);
        createAlert();
        GUI.getChildren().add(new Text("    Please click with the middle \n    mouse button on the map \n    to initialize surveillance area"));

    }

    public void createButtons(VBox container){
        HBox linesRow = new HBox();
        linesRow.setSpacing(3);

        int index = 0;


        for (String line : lines) {
            Button button = new Button();
            button.setText(line);
            button.setOnAction(this);
            button.setStyle("-fx-background-color: rgba(255,255,255,0.76);");
            button.setPrefWidth(40);
            linesRow.getChildren().add(button);
            if (++index % 5 == 0) {
                container.getChildren().add(linesRow);
                linesRow = new HBox();
                linesRow.setSpacing(3);
            }
        }
        container.getChildren().add(linesRow);
    }
    public void createRefreshSegment(VBox container){
        VBox refreshInterval = new VBox();
        refreshInterval.setPadding(new Insets(40, 0, 0, 0));
        refreshInterval.setSpacing(5);

        Text text = new Text();
        text.setText("               Refresh Interval [s]");
        refreshInterval.getChildren().add(text);

        Slider intervalSlider = createGenericSlider();

        intervalSlider.setOnMouseReleased(intervalMouseHandler);
        intervalSlider.setOnKeyReleased(intervalKeyboardHandler);

        refreshInterval.getChildren().add(intervalSlider);
        container.getChildren().add(refreshInterval);
    }
    public void createAreaSegment(VBox container){
        VBox areaRadius = new VBox();
        areaRadius.setPadding(new Insets(20, 0, 0, 0));
        areaRadius.setSpacing(5);

        Text text = new Text();
        text.setText("                   Area radius");

        areaRadius.getChildren().add(text);

        Slider radiusSlider = createGenericSlider();
        radiusSlider.setOnMouseReleased(radiusMouseHandler);
        radiusSlider.setOnKeyReleased(radiusKeyboardHandler);

        areaRadius.getChildren().add(radiusSlider);
        container.getChildren().add(areaRadius);
    }
    public void createAlert(){
        StackPane busAlert = new StackPane();
        Text alert = new Text("Desired vehicle in the area!");

        busAlert.getChildren().add(alert);
        busAlert.setVisible(false);
        StackPane.setAlignment(alert, Pos.CENTER);
        busAlert.setStyle("""
                        -fx-background-size: 1200 900;
                        -fx-background-radius: 30;
                        -fx-border-radius: 30;
                        -fx-border-width:5;
                        -fx-background-color: rgb(246,244,244);-fx-border-color: rgb(147,6,6);
                """);
        root.getChildren().add(busAlert);
        StackPane.setAlignment(busAlert, Pos.TOP_CENTER);
        busAlert.setMaxSize(root.getWidth() / 8, root.getHeight() / 15);
    }
    public Slider createGenericSlider() {
        Slider slider = new Slider();

        slider.setMin(1);
        slider.setMax(10);
        slider.setValue(5);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(5);
        slider.setBlockIncrement(1);
        slider.setMaxWidth(400);
        slider.setStyle("-fx-background-color: rgb(116,243,94);");
        return slider;
    }

    /*
        ///////////////////////////////////////
            Handling surveillance area feature
        ///////////////////////////////////////
    */

    private void createPolygon(double xCenter , double yCenter){
        Point2D graphicPoint = new Point2D(xCenter, yCenter);
        Point mapPoint = mapView.screenToLocation(graphicPoint);

        String coordinatesString = CoordinateFormatter.toLatitudeLongitude(mapPoint, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES, 6);
        String[] splitCoordinates = coordinatesString.split(" ");
        double lat = Double.parseDouble(splitCoordinates[0].replace("N", ""));
        double lon = Double.parseDouble((splitCoordinates[1].replace("E", "")));

        double x_onCircle;
        double y_onCircle;

        PointCollection points = new PointCollection(SpatialReferences.getWgs84());

        for (int i = 0; i < 31; i++) {
            x_onCircle = lon + (areaRadius * 0.0015 * Math.cos(i * ((2 * Math.PI) / 30)));
            y_onCircle = lat + (areaRadius * 0.0015 * 0.62 * Math.sin(i * ((2 * Math.PI) / 30)));
            points.add(new Point(x_onCircle, y_onCircle));
        }

        surveillanceArea = new Polygon(points);
    }
    public void setAlertVisible(boolean arg) {
        root.getChildren().stream()
                .filter(x -> x instanceof StackPane)
                .map(x -> (StackPane) x)
                .findFirst()
                .ifPresent(alert -> alert.setVisible(arg));
    }
    private void addWatchedPoints() {
        referencePoints.clear();
        Point point;
        for (Vehicle vehicle : vehiclesToPrint) {
            point = new Point(vehicle.x, vehicle.y, SpatialReferences.getWgs84());
            if (surveillancedLines.contains(vehicle.line)) {
                referencePoints.add(point);
            }
        }
    }
    public boolean isBusInSurArea() {
        if (surveillanceArea != null) {
            for (Graphic graphic : overlay.getGraphics()) {
                if (graphic.getGeometry().getGeometryType().equals(GeometryType.POINT)) {
                    if (GeometryEngine.contains(surveillanceArea, graphic.getGeometry())) {
                        if (referencePoints.size() != 0) {
                            for (Point point : referencePoints) {
                                if (GeometryEngine.equals(graphic.getGeometry(), point)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /*
        //////////////////////////////////////////////////////////////////////
            Getting vehicle data, creating and refreshing markers on the map
        /////////////////////////////////////////////////////////////////////
    */

    public void updateVehicles() {
        vehiclesToPrint.clear();
        try {
            vehiclesToPrint.addAll(Vehicle.createVehicles(JSonParser.parseVehicles(MPKApiConnection.getVehiclesData(activeLines))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void createElements() throws IOException, InterruptedException {
        overlay.getGraphics().clear();


        if (surveillanceArea != null) {
            SimpleLineSymbol redLine = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xffff0000, 2);
            SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0x200000FF, redLine);
            Graphic polygonGraphic = new Graphic(surveillanceArea, polygonSymbol);
            polygonGraphic.setZIndex(2);

            overlay.getGraphics().add(polygonGraphic);
        }

        SimpleMarkerSymbol busMarker = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, 0xffaaaaaa, 13);
        SimpleLineSymbol redOutline = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, 0xFFFF0000, 2);
        busMarker.setOutline(redOutline);

        SimpleMarkerSymbol tramMarker = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, 0xffaaaaaa, 13);
        SimpleLineSymbol blueOutline = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, 0xFF0000FF, 2);
        tramMarker.setOutline(blueOutline);

        Graphic pointGraphic;

        for (Vehicle vehicle : vehiclesToPrint) {
            Point point = new Point(vehicle.x, vehicle.y, SpatialReferences.getWgs84());
            referencePoints.add(point);
            if (vehicle.getType().equals("tram"))
                pointGraphic = new Graphic(point, tramMarker);
            else
                pointGraphic = new Graphic(point, busMarker);

            pointGraphic.setZIndex(0);
            overlay.getGraphics().add(pointGraphic);

            TextSymbol textSymbol = new TextSymbol();
            textSymbol.setText(vehicle.getLine().toUpperCase());
            textSymbol.setFontWeight(TextSymbol.FontWeight.BOLD);
            textSymbol.setSize(8);

            Graphic pointText = new Graphic(point, textSymbol);
            pointText.setZIndex(1);

            overlay.getGraphics().add(pointText);
        }
        addWatchedPoints();
        setAlertVisible(isBusInSurArea());
    }
    public void refreshElements() {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleWithFixedDelay(() -> {
            updateVehicles();
            try {
                createElements();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, refreshInterval, TimeUnit.SECONDS);
    }

    /*
        //////////////////////////
                Handlers
        //////////////////////////
    */


    @Override
    public void handle(javafx.event.ActionEvent event) {
        var line = ((Button) event.getSource()).getText().toLowerCase();
        if (activeLines.contains(line)) {
            activeLines.remove(line);
            ((Button) event.getSource()).setStyle("-fx-background-color: rgba(255,255,255,0.76);");
        } else {
            if (!((Button) event.getSource()).getText().equals("A") && !((Button) event.getSource()).getText().equals("C") && !((Button) event.getSource()).getText().equals("D") && !((Button) event.getSource()).getText().equals("K") && !((Button) event.getSource()).getText().equals("N")) {
                if (Integer.parseInt(((Button) event.getSource()).getText()) < 75)
                    ((Button) event.getSource()).setStyle("-fx-background-color: rgba(23,117,219,0.92);");
                else ((Button) event.getSource()).setStyle("-fx-background-color: rgba(184,6,13,0.92);");
            } else ((Button) event.getSource()).setStyle("-fx-background-color: rgba(184,6,13,0.92);");
            activeLines.add(line);
        }

        updateVehicles();
        try {
            createElements();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    EventHandler<? super MouseEvent> mapMouseHandler = (EventHandler<MouseEvent>) event -> {
        if (event.getButton().equals(MouseButton.MIDDLE)) {
            surveillancedLines.clear();

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("");
            dialog.setHeaderText("Please select lines to be surveillanced");
            dialog.setContentText("Separate each line with a comma ',': ");

            final boolean[] actionCanceled = {false};

            while (surveillancedLines.size() == 0 && !actionCanceled[0]) {
                Optional<String> result = dialog.showAndWait();

                result.ifPresentOrElse(x ->{
                    var input = x.split(",");
                    for (var arg : input) {
                        for (var line : lines) {
                            if (line.equalsIgnoreCase(arg)) {
                                surveillancedLines.add(arg.toLowerCase());
                            }
                        }
                        if(surveillancedLines.size()!=0)
                            createPolygon(event.getX(), event.getY());
                    }
                }, () -> actionCanceled[0] = true);
            }

            if(!actionCanceled[0]) {
                try {
                    createElements();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                addWatchedPoints();
                setAlertVisible(isBusInSurArea());
            }
        }
    };

    EventHandler<? super MouseEvent> intervalMouseHandler = (EventHandler<MouseEvent>) event -> {
        executor.shutdown();
        refreshInterval = (long) ((Slider) event.getSource()).getValue();
        refreshElements();
    };

    EventHandler<? super KeyEvent> intervalKeyboardHandler = (EventHandler<KeyEvent>) event -> {
        executor.shutdown();
        refreshInterval = (long) ((Slider) event.getSource()).getValue();
        refreshElements();
    };

    EventHandler<? super MouseEvent> radiusMouseHandler = event -> areaRadius = (long) ((Slider) event.getSource()).getValue();

    EventHandler<? super KeyEvent> radiusKeyboardHandler = event -> areaRadius = (long) ((Slider) event.getSource()).getValue();
}