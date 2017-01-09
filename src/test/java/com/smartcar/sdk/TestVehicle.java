package com.smartcar.sdk;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;

public class TestVehicle {

  final String API_PATH = "/v1.0/vehicles";
  final String ACCESS_TOKEN = "access-token";
  final String VEHICLE_ID = "vehicle-id";
  final String AUTHORIZATION = "Bearer " + ACCESS_TOKEN;
  final String SUCCESS = "{\"status\":\"success\"}";

  private final Gson gson = new Gson();
  Vehicle vehicle = new Vehicle(VEHICLE_ID, ACCESS_TOKEN);
  MockWebServer server;
  RecordedRequest request;

  /* create a new server before every test */
  @BeforeMethod
  public void before() {
    server = new MockWebServer();
  }

  /* shutdown the server after every test */
  @AfterMethod
  public void after() throws IOException{
    server.shutdown();
  }

  /* set the next response using body, start the server, and set the
    API url to the url generated by MockWebServer */
  private void setup(String body) {
    System.out.println(body);
    server.enqueue(new MockResponse().setBody(body));
    try {
      server.start();
    } catch (IOException e) {
      System.out.println(e);
    }
    vehicle.setBaseUrl(server.url(API_PATH).toString());
  }

  /* set the global request object to the next request */
  private void updateRequest() {
    try {
      request = server.takeRequest();
    } catch (InterruptedException e) {
      System.out.println(e);
    }
  }

  /* verify the auth header, HTTP method, and request path */
  private void verify(String method, String path) {
    updateRequest();
    Assert.assertEquals(
      request.getHeader("Authorization"), 
      AUTHORIZATION
    );
    Assert.assertEquals(request.getMethod(), method);
    Assert.assertEquals(
      request.getPath(), 
      API_PATH + '/' + VEHICLE_ID + '/' + path
    );
  }

  /* verify(method, path) and action */
  private void verify(String method, String path, String action) {
    verify(method, path);
    Assert.assertEquals(
      gson.fromJson(
        request.getBody().readUtf8(),
        Api.GenericAction.class
      ).action,
      action
    );   
  }

  /* Get Intent Tests */
  @Test public void testPermissions()
  throws Exceptions.SmartcarException {
    setup("{\"permissions\":[\"read_vehicle_info\"]}");
    String[] permissions = vehicle.permissions().permissions;
    verify("GET", "permissions");
    Assert.assertEquals(permissions[0], "read_vehicle_info");
  }

  @Test public void testPermissionsWithPaging()
  throws Exceptions.SmartcarException {
    setup("{\"permissions\":[\"read_vehicle_info\"]}");
    String[] permissions = vehicle.permissions(10, 0).permissions;
    verify("GET", "permissions?limit=10&offset=0");
    Assert.assertEquals(
      permissions[0], 
      "read_vehicle_info"
    );    
  }

  @Test public void testInfo()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("id", VEHICLE_ID)
      .put("make", "MAKE")
      .put("model", "MODEL")
      .put("year", 2016)
      .toString());
    Api.Info data = vehicle.info();
    verify("GET", "");
    Assert.assertEquals(data.id, VEHICLE_ID);
    Assert.assertEquals(data.make, "MAKE");
    Assert.assertEquals(data.model, "MODEL");
    Assert.assertEquals(data.year, 2016);
  }

  @Test public void testAccelerometer()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("x", 100.0)
      .put("y", 200.0)
      .put("z", 300.0)
      .toString());
    Api.Accelerometer data = vehicle.accelerometer();
    verify("GET", "accelerometer");
    Assert.assertEquals(data.x, 100.0);
    Assert.assertEquals(data.y, 200.0);
    Assert.assertEquals(data.z, 300.0);
  }

  @Test public void testAirbags()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("airbags", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("isActive", true)
          .put("isDeployed", true)
        )
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("isActive", true)
          .put("isDeployed", false)
        )
      )
      .toString());
    Api.Airbag[] data = vehicle.airbags().airbags;
    verify("GET", "airbags");
    Assert.assertEquals(data[0].location, "FRONT_RIGHT");
    Assert.assertEquals(data[0].isActive, true);
    Assert.assertEquals(data[0].isDeployed, true);
    Assert.assertEquals(data[1].location, "FRONT_LEFT");
    Assert.assertEquals(data[1].isActive, true);
    Assert.assertEquals(data[1].isDeployed, false);
  }

  @Test public void testBarometer()
  throws Exceptions.SmartcarException {
    setup("{\"pressure\": 123.4 }");
    Api.Barometer data = vehicle.barometer();
    verify("GET", "barometer");
    Assert.assertEquals(data.pressure, 123.4);
  }

  @Test public void testBattery()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("range", 40.5)
      .put("percentRemaining", 0.5)
      .toString());
    Api.Battery data = vehicle.battery();
    verify("GET", "battery");
    Assert.assertEquals(data.range, 40.5);
    Assert.assertEquals(data.percentRemaining, 0.5);
  }

  @Test public void testCharge() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isPluggedIn", true)
      .put("state", "CHARGING")
      .toString());
    Api.Charge data = vehicle.charge();
    verify("GET", "charge");
    Assert.assertEquals(data.isPluggedIn, true);
    Assert.assertEquals(data.state, "CHARGING");
  }

  @Test public void testChargeLimit()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("limit", 0.6)
      .put("state", "ENABLED")
      .toString());
    Api.ChargeLimit data = vehicle.chargeLimit();
    verify("GET", "charge/limit");
    Assert.assertEquals(data.state, "ENABLED");
    Assert.assertEquals(data.limit, 0.6);
  }

  @Test public void testChargeSchedule()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("state", "ENABLED")
      .put("startTime", "12:30")
      .toString());
    Api.ChargeSchedule data = vehicle.chargeSchedule();
    verify("GET", "charge/schedule");
    Assert.assertEquals(data.state, "ENABLED");
    Assert.assertEquals(data.startTime, "12:30");
  }

  @Test public void testClimate() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("temperature", 100.6)
      .put("isOn", false)
      .toString());
    Api.Climate data = vehicle.climate();
    verify("GET", "climate");
    Assert.assertEquals(data.temperature, 100.6);
    Assert.assertEquals(data.isOn, false);
  }

  @Test public void testCollisionSensor() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isTriggered", true)
      .toString());
    Api.CollisionSensor data = vehicle.collisionSensor();
    verify("GET", "collision_sensor");
    Assert.assertEquals(data.isTriggered, true);
  }

  @Test public void testCompass() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("heading", 40.5)
      .toString());
    Api.Compass data = vehicle.compass();
    verify("GET", "compass");
    Assert.assertEquals(data.heading, 40.5);
  }

  @Test public void testCruiseControl() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("speed", 100.5)
      .put("followDistance", 123.4)
      .put("isOn", false)
      .toString());
    Api.CruiseControl data = vehicle.cruiseControl();
    verify("GET", "cruise_control");
    Assert.assertEquals(data.speed, 100.5);
    Assert.assertEquals(data.followDistance, 123.4);
    Assert.assertEquals(data.isOn, false);
  }

  @Test public void testDimensions() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("height", 11.1)
      .put("width", 12.2)
      .put("length", 13.3)
      .put("weight", 1234.5)
      .toString());
    Api.Dimensions data = vehicle.dimensions();
    verify("GET", "dimensions");
    Assert.assertEquals(data.height, 11.1);
    Assert.assertEquals(data.width, 12.2);
    Assert.assertEquals(data.length, 13.3);
    Assert.assertEquals(data.weight, 1234.5);
  }

  @Test public void testDoors() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("doors", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("isOpen", false)
        )
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("isOpen", true)
        )
      )
      .toString());
    Api.Doors data = vehicle.doors();
    verify("GET", "doors");
    Assert.assertEquals(data.doors[0].location, "FRONT_RIGHT");
    Assert.assertEquals(data.doors[0].isOpen, false);
    Assert.assertEquals(data.doors[1].location, "FRONT_LEFT");
    Assert.assertEquals(data.doors[1].isOpen, true);
  }

  @Test public void testChildSafetyLocks()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("childSafetyLocks", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("isLocked", false)
        )
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("isLocked", true)
        )
      )
      .toString());
    Api.ChildSafetyLock[] data = vehicle.childSafetyLocks().childSafetyLocks;
    verify("GET", "doors/child_safety_locks");
    Assert.assertEquals(data[0].location, "FRONT_LEFT");
    Assert.assertEquals(data[0].isLocked, false);
    Assert.assertEquals(data[1].location, "FRONT_RIGHT");
    Assert.assertEquals(data[1].isLocked, true);
  }

  @Test public void testDriveMode() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("mode", "MODE")
      .toString());
    Api.DriveMode data = vehicle.driveMode();
    verify("GET", "drive_mode");
    Assert.assertEquals(data.mode, "MODE");
  }

  @Test public void testEngine() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isOn", "IS_ON")
      .toString());
    Api.Engine data = vehicle.engine();
    verify("GET", "engine");
    Assert.assertEquals(data.isOn, "IS_ON");
  }

  @Test public void testEngineCoolant() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("level", 0.5)
      .put("temperature", 100.5)
      .toString());
    Api.EngineCoolant data = vehicle.engineCoolant();
    verify("GET", "engine/coolant");
    Assert.assertEquals(data.level, 0.5);
    Assert.assertEquals(data.temperature, 100.5);
  }

  @Test public void testEngineHood() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isOpen", false)
      .toString());
    Api.EngineHood data = vehicle.engineHood();
    verify("GET", "engine/hood");
    Assert.assertEquals(data.isOpen, false);
  }

  @Test public void testEngineOil() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("changeIndicator", false)
      .put("percentRemaining", 0.5)
      .put("lifeRemaining", 0.3)
      .put("pressure", 100.4)
      .put("temperature", 200.5)
      .toString());
    Api.EngineOil data = vehicle.engineOil();
    verify("GET", "engine/oil");
    Assert.assertEquals(data.changeIndicator, false);
    Assert.assertEquals(data.percentRemaining, 0.5);
    Assert.assertEquals(data.lifeRemaining, 0.3);
    Assert.assertEquals(data.pressure, 100.4);
    Assert.assertEquals(data.temperature, 200.5);
  }

  @Test public void testEngineThrottle() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("percentOpen", 0.5)
      .toString());
    Api.EngineThrottle data = vehicle.engineThrottle();
    verify("GET", "engine/throttle");
    Assert.assertEquals(data.percentOpen, 0.5);
  }

  @Test public void testFuel() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("range", 100.5)
      .put("percentRemaining", 0.7)
      .toString());
    Api.Fuel data = vehicle.fuel();
    verify("GET", "fuel");
    Assert.assertEquals(data.range, 100.5);
    Assert.assertEquals(data.percentRemaining, 0.7);
  }

  @Test public void testGyroscope()
          throws Exceptions.SmartcarException {
    setup(new JSONObject()
            .put("yawRate", 20.2)
            .toString());
    Api.Gyroscope data = vehicle.gyroscope();
    verify("GET", "gyroscope");
    Assert.assertEquals(data.yawRate, 20.2);
  }

  @Test public void testHazardLights()
  throws Exceptions.SmartcarException {
    setup(new JSONObject().put("isOn", false).toString());
    Api.HazardLight data = vehicle.hazardLights();
    verify("GET", "lights/hazard");
    Assert.assertEquals(data.isOn, false);
  }

  @Test public void testHeadlights() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject().put("state", "STATE").toString());
    Api.Headlight data = vehicle.headlights();
    verify("GET", "lights/headlights");
    Assert.assertEquals(data.state, "STATE");
  }

  @Test public void testIgnition()
  throws Exceptions.SmartcarException {
    setup(new JSONObject().put("state", "STATE").toString());
    Api.Ignition data = vehicle.ignition();
    verify("GET", "ignition");
    Assert.assertEquals(data.state, "STATE");
  }

  @Test public void testInteriorLights() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("lights", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("isOn", true)
        )
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("isOn", false)
        )
      ).toString());
    Api.InteriorLight[] data = vehicle.interiorLights().lights;
    verify("GET", "lights/interior");
    Assert.assertEquals(data[0].location, "FRONT_LEFT");
    Assert.assertEquals(data[0].isOn, true);
    Assert.assertEquals(data[1].location, "FRONT_RIGHT");
    Assert.assertEquals(data[1].isOn, false);
  }

  @Test public void testTurnIndicator() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("state", "LEFT")
      .toString());
    Api.TurnIndicator data = vehicle.turnIndicator();
    verify("GET", "lights/turn_indicator");
    Assert.assertEquals(data.state, "LEFT");
  }

  @Test public void testLocation() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("latitude", 40.5)
      .put("longitude", 38.2)
      .put("accuracy", 102.2)
      .toString());
    Api.Location data = vehicle.location();
    verify("GET", "location");
    Assert.assertEquals(data.latitude, 40.5);
    Assert.assertEquals(data.longitude, 38.2);
  }

  @Test public void testLocationNoAccuracy() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("latitude", 40.5)
      .put("longitude", 38.2)
      .toString());
    Api.Location data = vehicle.location();
    verify("GET", "location");
    Assert.assertEquals(data.latitude, 40.5);
    Assert.assertEquals(data.longitude, 38.2);
  }

  @Test public void testSideviewMirrors()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("mirrors", new JSONArray()
        .put(new JSONObject()
          .put("location", "LEFT")
          .put("xTilt", 0.1)
          .put("yTilt", -0.5)
        )
        .put(new JSONObject()
          .put("location", "RIGHT")
          .put("xTilt", 0.2)
          .put("yTilt", 0.3)
        )
      )
      .toString());
    Api.Mirror[] data = vehicle.sideviewMirrors().mirrors;
    verify("GET", "mirrors/side_view");
    Assert.assertEquals(data[0].location, "LEFT");
    Assert.assertEquals(data[1].location, "RIGHT");
    Assert.assertEquals(data[0].xTilt, 0.1);
    Assert.assertEquals(data[1].xTilt, 0.2);
    Assert.assertEquals(data[0].yTilt, -0.5);
    Assert.assertEquals(data[1].yTilt, 0.3);
  }

  @Test public void testOdometer() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("distance", 104.32)
      .toString());
    Api.Odometer data = vehicle.odometer();
    verify("GET", "odometer");
    Assert.assertEquals(data.distance, 104.32);
  }

  @Test public void testTripOdometers() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("trips", new JSONArray()
        .put(new JSONObject()
          .put("label", "a")
          .put("distance", 101.12)
        )
        .put(new JSONObject()
          .put("label", "b")
          .put("distance", 23.11)
        )
      )
      .toString());
    Api.TripOdometer[] data = vehicle.tripOdometers().trips;
    verify("GET", "odometer/trips");
    Assert.assertEquals(data[0].label, "a");
    Assert.assertEquals(data[0].distance, 101.12);
    Assert.assertEquals(data[1].label, "b");
    Assert.assertEquals(data[1].distance, 23.11);
  }

  @Test public void testAcceleratorPedal() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("percentDepressed", 0.3)
      .toString());
    Api.Pedal data = vehicle.acceleratorPedal();
    verify("GET", "pedals/accelerator");
    Assert.assertEquals(data.percentDepressed, 0.3);
  }

  @Test public void testBrakePedal() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("percentDepressed", 0.3)
      .toString());
    Api.Pedal data = vehicle.brakePedal();
    verify("GET", "pedals/brake");
    Assert.assertEquals(data.percentDepressed, 0.3);
  }

  @Test public void testRainSensor() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isRaining", false)
      .toString());
    Api.RainSensor data = vehicle.rainSensor();
    verify("GET", "rain_sensor");
    Assert.assertEquals(data.isRaining, false);
  }

  @Test public void testSeats() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("seats", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("isOccupied", true)
          .put("isBuckled", false)
        )
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("isOccupied", false)
          .put("isBuckled", true)
        )
      ).toString());
    Api.Seat[] data = vehicle.seats().seats;
    verify("GET", "seats");
    Assert.assertEquals(data[0].location, "FRONT_LEFT");
    Assert.assertEquals(data[0].isOccupied, true);
    Assert.assertEquals(data[0].isBuckled, false);
    Assert.assertEquals(data[1].location, "FRONT_RIGHT");
    Assert.assertEquals(data[1].isOccupied, false);
    Assert.assertEquals(data[1].isBuckled, true);
  }

  @Test public void testSecurity() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isLocked", true)
      .toString());
    Api.Security data = vehicle.security();
    verify("GET", "security");
    Assert.assertEquals(data.isLocked, true);
  }

  @Test public void testSliBattery() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("current", 15.5)
      .put("voltage", 12.2)
      .put("percentRemaining", 0.7)
      .toString());
    Api.SliBattery data = vehicle.sliBattery();
    verify("GET", "sli_battery");
    Assert.assertEquals(data.current, 15.5);
    Assert.assertEquals(data.voltage, 12.2);
    Assert.assertEquals(data.percentRemaining, 0.7);
  }

  @Test public void testSpeedometer() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("speed", 100.2)
      .toString());
    Api.Gauge data = vehicle.speedometer();
    verify("GET", "speedometer");
    Assert.assertEquals(data.speed, 100.2);
  }

  @Test public void testSteeringWheel() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("location", "LEFT")
      .put("angle", 100.2)
      .toString());
    Api.SteeringWheel data = vehicle.steeringWheel();
    verify("GET", "steering_wheel");
    Assert.assertEquals(data.location, "LEFT");
    Assert.assertEquals(data.angle, 100.2);
  }

  @Test public void testSunroof() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("state", "OPEN")
      .put("percentOpen", 0.5)
      .toString());
    Api.Sunroof data = vehicle.sunroof();
    verify("GET", "sunroof");
    Assert.assertEquals(data.state, "OPEN");
    Assert.assertEquals(data.percentOpen, 0.5);
  }

  @Test public void testTachometer() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("engineSpeed", 1500.5)
      .toString());
    Api.EngineSpeed data = vehicle.tachometer();
    verify("GET", "tachometer");
    Assert.assertEquals(data.engineSpeed, 1500.5);
  }

  @Test public void testInteriorThermistor()
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("temperature", 86.2)
      .toString());
    Api.Temperature data = vehicle.interiorThermistor();
    verify("GET", "thermistors/interior");
    Assert.assertEquals(data.temperature, 86.2);
  }

  @Test public void testExteriorThermistor()
          throws Exceptions.SmartcarException {
    setup(new JSONObject()
            .put("temperature", 90.2)
            .toString());
    Api.Temperature data = vehicle.exteriorThermistor();
    verify("GET", "thermistors/exterior");
    Assert.assertEquals(data.temperature, 90.2);
  }

  @Test public void testTires() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("tires", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("pressure", 200.5)
        )
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("pressure", 200.5)
        )
      ).toString());
    Api.Tire[] data = vehicle.tires().tires;
    verify("GET", "tires");
    Assert.assertEquals(data[0].location, "FRONT_LEFT");
    Assert.assertEquals(data[1].location, "FRONT_RIGHT");
    Assert.assertEquals(data[0].pressure, data[1].pressure);
  }

  @Test public void testTransmission() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("type", "AUTOMATIC")
      .put("state", "PARKED")
      .toString());
    Api.Transmission data = vehicle.transmission();
    verify("GET", "transmission");
    Assert.assertEquals(data.type, "AUTOMATIC");
    Assert.assertEquals(data.state, "PARKED");
  }

  @Test public void testTransmissionFluid() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("temperature", 100.6)
      .put("wear", 0.9)
      .toString());
    Api.TransmissionFluid data = vehicle.transmissionFluid();
    verify("GET", "transmission/fluid");
    Assert.assertEquals(data.temperature, 100.6);
    Assert.assertEquals(data.wear, 0.9);
  }

  @Test public void testFrontTrunk() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isOpen", false)
      .toString());
    Api.Trunk data = vehicle.frontTrunk();
    verify("GET", "trunks/front");
    Assert.assertEquals(data.isOpen, false);
  }

  @Test public void testRearTrunk() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("isOpen", true)
      .toString());
    Api.Trunk data = vehicle.rearTrunk();
    verify("GET", "trunks/rear");
    Assert.assertEquals(data.isOpen, true);
  }

  @Test public void testVin() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("vin", "VIN")
      .toString());
    Api.Vin data = vehicle.vin();
    verify("GET", "vin");
    Assert.assertEquals(data.vin, "VIN");
  }

  @Test public void testWasherFluid() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("percentRemaining", 0.3)
      .toString());
    Api.WasherFluid data = vehicle.washerFluid();
    verify("GET", "washer_fluid");
    Assert.assertEquals(data.percentRemaining, 0.3);
  }

  @Test public void testWheels() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("wheels", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("diameter", 48.26)
        )
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("diameter", 48.26)
        )
      ).toString());
    Api.Wheels data = vehicle.wheels();
    verify("GET", "wheels");
    Assert.assertEquals(data.wheels[0].location, "FRONT_RIGHT");
    Assert.assertEquals(data.wheels[1].location, "FRONT_LEFT");
    Assert.assertEquals(data.wheels[0].diameter, data.wheels[1].diameter);
  }

  @Test public void testWheelSpeeds() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("wheelSpeed", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("speed", 20.52)
        )
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("speed", 20.52)
        )
      ).toString());
    Api.WheelSpeeds data = vehicle.wheelSpeeds();
    verify("GET", "wheels/speeds");
    Assert.assertEquals(data.wheelSpeed[0].location, "FRONT_RIGHT");
    Assert.assertEquals(data.wheelSpeed[1].location, "FRONT_LEFT");
    Assert.assertEquals(data.wheelSpeed[0].speed, data.wheelSpeed[1].speed); 
  }

  @Test public void testWindows() 
  throws Exceptions.SmartcarException {
    setup(new JSONObject()
      .put("windows", new JSONArray()
        .put(new JSONObject()
          .put("location", "FRONT_RIGHT")
          .put("isLocked", true)
          .put("percentOpen", 0.5)
        )
        .put(new JSONObject()
          .put("location", "FRONT_LEFT")
          .put("isLocked", false)
          .put("percentOpen", 0.0)
        )
      ).toString());
    Api.Window[] data = vehicle.windows().windows;
    verify("GET", "windows");
    Assert.assertEquals(data[0].location, "FRONT_RIGHT");
    Assert.assertEquals(data[0].isLocked, true);
    Assert.assertEquals(data[0].percentOpen, 0.5);
    Assert.assertEquals(data[1].location, "FRONT_LEFT");
    Assert.assertEquals(data[1].isLocked, false);
    Assert.assertEquals(data[1].percentOpen, 0.0);
  }

  /* Action Intent Tests */

  @Test public void testDisconnect() 
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.disconnect();
    verify("DELETE", "application");
    Assert.assertEquals(request.getBody().readUtf8(), "");
  }

  @Test public void testStartCharging() 
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.startCharging();
    verify("POST", "charge", "START");
  }

  @Test public void testStopCharging() 
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.stopCharging();
    verify("POST", "charge", "STOP");
  }

  @Test public void testEnableChargeLimitNoParameter() 
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.enableChargeLimit();
    verify("POST", "charge/limit", "ENABLE");
  }

  @Test public void testEnableChargeLimitWithParameter()
  throws Exceptions.SmartcarException {
    double LIMIT = 0.8;
    setup(SUCCESS);
    vehicle.enableChargeLimit(LIMIT);
    verify("POST", "charge/limit");
    Api.ChargeLimitAction action = gson.fromJson(
      request.getBody().readUtf8(), 
      Api.ChargeLimitAction.class
    );
    Assert.assertEquals(action.action, "ENABLE");
    Assert.assertEquals(action.limit, LIMIT);
  }

  @Test public void testDisableChargeLimit() 
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.disableChargeLimit();
    verify("POST", "charge/limit", "DISABLE");
  }

  @Test public void testEnableChargeScheduleNoParameter()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.enableChargeSchedule();
    verify("POST", "charge/schedule", "ENABLE");
  }

  @Test public void testEnableChargeScheduleWithParameter()
  throws Exceptions.SmartcarException {
    String TIME = "12:30";
    setup(SUCCESS);
    vehicle.enableChargeSchedule(TIME);
    verify("POST", "charge/schedule");
    Api.ChargeScheduleAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.ChargeScheduleAction.class
    );
    Assert.assertEquals(action.action, "ENABLE");
    Assert.assertEquals(action.startTime, TIME);
  }

  @Test public void testDisableChargeSchedule()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.disableChargeSchedule();
    verify("POST", "charge/schedule", "DISABLE");
  }

  @Test public void testStartClimateNoParameter()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.startClimate();
    verify("POST", "climate", "START");
  }

  @Test public void testStartClimateWithParameter()
  throws Exceptions.SmartcarException {
    double TEMPERATURE = 1;
    setup(SUCCESS);
    vehicle.startClimate(TEMPERATURE);
    verify("POST", "climate");
    Api.ClimateAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.ClimateAction.class
    );
    Assert.assertEquals(action.action, "START");
    Assert.assertEquals(action.temperature, TEMPERATURE);
  }

  @Test public void testStopClimate()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.stopClimate();
    verify("POST", "climate", "STOP");
  }

  @Test public void testStartIgnition()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.startIgnition();
    verify("POST", "ignition", "START");
  }

  @Test public void testSetIgnitionOff()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.setIgnitionOff();
    verify("POST", "ignition", "OFF");
  }

  @Test public void testSetIgnitionOn()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.setIgnitionOn();
    verify("POST", "ignition", "ON");
  }

  @Test public void testSetIgnitionAccessory()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.setIgnitionAccessory();
    verify("POST", "ignition", "ACCESSORY");
  }

  @Test public void testOpenEngineHood()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.openEngineHood();
    verify("POST", "engine/hood", "OPEN");
  }

  @Test public void testCloseEngineHood()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.closeEngineHood();
    verify("POST", "engine/hood", "CLOSE");
  }

  @Test public void testHonkHorn()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.honkHorn();
    verify("POST", "horn", "HONK");
  }

  @Test public void testFlashHeadlights()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.flashHeadlight();
    verify("POST", "lights/headlights", "FLASH");
  }

  @Test public void testTiltSideviewMirrors()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    Api.Mirror[] mirrors = {
      new Api.Mirror("LEFT", 0.1, -0.07),
      new Api.Mirror("RIGHT", 0.2, 0.5)
    };
    vehicle.tiltSideviewMirrors(mirrors);
    verify("POST", "mirrors/side_view");
    Api.MirrorAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.MirrorAction.class
    );
    Assert.assertEquals(action.action, "TILT");
    Assert.assertEquals(action.mirrors[0].location, "LEFT");
    Assert.assertEquals(action.mirrors[1].location, "RIGHT");
    Assert.assertEquals(action.mirrors[0].xTilt, 0.1);
    Assert.assertEquals(action.mirrors[1].xTilt, 0.2);
    Assert.assertEquals(action.mirrors[0].yTilt, -0.07);
    Assert.assertEquals(action.mirrors[1].yTilt, 0.5);
  }

  @Test public void testStartPanic()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.startPanic();
    verify("POST", "panic", "START");
  }

  @Test public void testStopPanic()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.stopPanic();
    verify("POST", "panic", "STOP");
  }

  @Test public void testOpenChargePort()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.openChargePort();
    verify("POST", "ports/charge", "OPEN");
  }

  @Test public void testCloseChargePort()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.closeChargePort();
    verify("POST", "ports/charge", "CLOSE");
  }

  @Test public void testOpenFuelPort()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.openFuelPort();
    verify("POST", "ports/fuel", "OPEN");
  }

  @Test public void testCloseFuelPort()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.closeFuelPort();
    verify("POST", "ports/fuel", "CLOSE");
  }

  @Test public void testLock()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.lock();
    verify("POST", "security", "LOCK");
  }

  @Test public void testUnlock()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.unlock();
    verify("POST", "security", "UNLOCK");
  }

  @Test public void testOpenSunroof()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.openSunroof();
    verify("POST", "sunroof", "OPEN");
  }

  @Test public void testVentSunroof()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.ventSunroof();
    verify("POST", "sunroof", "VENT");
  }

  @Test public void testCloseSunroof()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.closeSunroof();
    verify("POST", "sunroof", "CLOSE");
  }

  @Test public void testOpenFrontTrunk()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.openFrontTrunk();
    verify("POST", "trunks/front", "OPEN");
  }

  @Test public void testCloseFrontTrunk()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.closeFrontTrunk();
    verify("POST", "trunks/front", "CLOSE");
  }

  @Test public void testOpenRearTrunk()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.openRearTrunk();
    verify("POST", "trunks/rear", "OPEN");
  }
  @Test public void testCloseRearTrunk()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    vehicle.closeRearTrunk();
    verify("POST", "trunks/rear", "CLOSE");
  }

  @Test public void testOpenWindows()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    Api.Window[] windows = {
      new Api.Window("FRONT_LEFT", 0.5)
    };
    vehicle.openWindows(windows);
    verify("POST", "windows");
    Api.WindowAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.WindowAction.class
    );
    Assert.assertEquals(action.action, "OPEN");
    Assert.assertEquals(action.windows[0].location, "FRONT_LEFT");
    Assert.assertEquals(action.windows[0].percentOpen, 0.5);
  }

  @Test public void testCloseWindows()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    Api.Window[] windows = {
      new Api.Window("FRONT_LEFT")
    };
    vehicle.closeWindows(windows);
    verify("POST", "windows");
    Api.WindowAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.WindowAction.class
    );
    Assert.assertEquals(action.action, "CLOSE");
    Assert.assertEquals(action.windows[0].location, "FRONT_LEFT");
  }

  @Test public void testLockWindows()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    Api.Window[] windows = {
      new Api.Window("FRONT_LEFT")
    };
    vehicle.lockWindows(windows);
    verify("POST", "windows");
    Api.WindowAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.WindowAction.class
    );
    Assert.assertEquals(action.action, "LOCK");
    Assert.assertEquals(action.windows[0].location, "FRONT_LEFT");
  }

  @Test public void testUnlockWindows()
  throws Exceptions.SmartcarException {
    setup(SUCCESS);
    Api.Window[] windows = {
      new Api.Window("FRONT_LEFT")
    };
    vehicle.unlockWindows(windows);
    verify("POST", "windows");
    Api.WindowAction action = gson.fromJson(
      request.getBody().readUtf8(),
      Api.WindowAction.class
    );
    Assert.assertEquals(action.action, "UNLOCK");
    Assert.assertEquals(action.windows[0].location, "FRONT_LEFT");
  }
}
