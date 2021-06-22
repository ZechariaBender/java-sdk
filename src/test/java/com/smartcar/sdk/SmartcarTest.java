package com.smartcar.sdk;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smartcar.sdk.data.Compatibility;
import com.smartcar.sdk.data.User;
import com.smartcar.sdk.data.VehicleIds;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;

@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({
        System.class,
        Smartcar.class,
})
public class SmartcarTest extends PowerMockTestCase {
    private final String sampleClientId = "cl13nt1d-t35t-46dc-aa25-bdd042f54e7d";
    private final String sampleClientSecret = "24d55382-843f-4ce9-a7a7-cl13nts3cr3t";
    private final String sampleRequestId = "2eddd02f-8aaa-2eee-bfff-012345678901";
    private final String sampleRedirectUri = "https://example.com/";
    private final String sampleRedirectUriEncoded = "https%3A%2F%2Fexample.com%2F";
    private final String[] sampleScope = {"read_vehicle_info", "read_location", "read_odometer"};
    private final boolean sampleTestMode = true;
    private String fakeAccessToken = "F4K3_4CC355_T0K3N";

    @AfterTest
    public void afterTest() throws InterruptedException {
        //TestExecutionListener.mockWebServer.takeRequest();
    }

    @Test
    public void testGetUser() throws Exception {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("SMARTCAR_API_ORIGIN")).thenReturn(
                "http://localhost:" + TestExecutionListener.mockWebServer.getPort()
        );
        String expectedUserId = "9c58a58f-579e-4fce-b2fc-53a518271b8c";
        MockResponse response = new MockResponse()
                .setBody("{ \"id\": \"" + expectedUserId + "\" }")
                .addHeader("SC-Request-Id", this.sampleRequestId);
        TestExecutionListener.mockWebServer.enqueue(response);

        User user = Smartcar.getUser(this.fakeAccessToken);
        Assert.assertEquals(user.getId(), expectedUserId);
        Assert.assertEquals(user.getMeta().getRequestId(), this.sampleRequestId);
        TestExecutionListener.mockWebServer.takeRequest();
    }

    @Test
    public void testVehicles() throws Exception {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("SMARTCAR_API_ORIGIN")).thenReturn(
                "http://localhost:" + TestExecutionListener.mockWebServer.getPort()
        );
        String vehicleId = "9c58a58f-579e-4fce-b2fc-53a518271b8c";

        MockResponse response = new MockResponse()
                .setBody("{ \"paging\": {\"count\": 1, \"offset\": 0 }, \"vehicles\": [\"" + vehicleId + "\"] }")
                .addHeader("SC-Request-Id", this.sampleRequestId);
        TestExecutionListener.mockWebServer.enqueue(response);

        VehicleIds vehicleIds = Smartcar.getVehicles(this.fakeAccessToken);

        String[] vIds = vehicleIds.getVehicleIds();
        Assert.assertNotNull(vIds);
        Assert.assertEquals(vIds[0], vehicleId);
        TestExecutionListener.mockWebServer.takeRequest();
    }

    @Test
    public void testGetCompatibility() throws Exception {
        String vin = "";
        String[] scope;
        scope = new String[]{"read_odometer"};

        MockResponse response = new MockResponse()
                .setBody("{ \"compatible\": true }")
                .addHeader("SC-Request-Id", this.sampleRequestId);
        TestExecutionListener.mockWebServer.enqueue(response);

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("SMARTCAR_API_ORIGIN")).thenReturn(
                "http://localhost:" + TestExecutionListener.mockWebServer.getPort()
        );
        PowerMockito.when(System.getenv("SMARTCAR_CLIENT_ID")).thenReturn(this.sampleClientId);
        PowerMockito.when(System.getenv("SMARTCAR_CLIENT_SECRET")).thenReturn(this.sampleClientSecret);

        SmartcarCompatibilityRequest request =  new SmartcarCompatibilityRequest.Builder()
                .clientId(this.sampleClientId)
                .clientSecret(this.sampleClientSecret)
                .vin(vin)
                .scope(scope)
                .build();
        Compatibility comp = Smartcar.getCompatibility(request);
        Assert.assertTrue(comp.getCompatible());
        Assert.assertEquals(comp.getMeta().getRequestId(), this.sampleRequestId);
        TestExecutionListener.mockWebServer.takeRequest();
    }

    /**
     * Tests setting the api version to 2.0 and getting the api url that is used for subsequent
     * requests
     */
    @Test
    public void testSetApiVersion() {
        Smartcar.setApiVersion("2.0");
        String url = Smartcar.getApiUrl();
        Assert.assertEquals(url, "https://api.smartcar.com/v2.0");
        Smartcar.setApiVersion("1.0");
    }
}
