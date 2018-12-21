package Airport.Airport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import Airplane.Aircraft.Airplane;
import Airport.AirCargoPalletLifter.AirCargoPalletLifter;
import Airport.AirCargoPalletLifter.AirCargoPalletVehicle;
import Airport.ApronControl.ApronControl;
import Airport.ApronControl.Apron;
import Airport.Baggage_Sorting_Unit.BaggageSortingUnit;
import Airport.Base.Passenger;
import Airport.Checkin_Desk.CheckInMediator;
import Airport.Configuration.Configuration;
import Airport.Customs.Customs;
import Airport.Federal_Police.FederalPolice;
import Airport.Pushback_Vehicle.PushBackVehicle;
import Airport.Scanner.BaggageScanner;
import Airport.Security_Check.SecurityMediator;
import Airport.Ground_Operations.GroundOperationsCenter;
import Airport.Bulky_Baggage_Desk.BulkyBaggageDesk;
import Airport.Base.Flight;
import Airport.Service_Vehicle.ServiceVehicleBase;
import Airport.Service_Vehicle.ServiceVehicleFreshWater;
import Airport.Service_Vehicle.ServiceVehicleNitrogenOxygen;
import Airport.Service_Vehicle.ServiceVehicleWasteWater;
import Airport.Sky_Tanking_Vehicle.SkyTankingVehicle;
import java.io.IOException;

import static Airport.Airport.GateID.A01;
import static Airport.Airport.GateID.GATE_ID;
import static Airport.Configuration.Configuration.DATAFILEPATH;

public class Airport{
    private ArrayList<Passenger> passengerList;
    private AirportResourcePool resourcePool;
    private ArrayList<Gate> gateList;
    private Apron apron;
    private GroundOperationsCenter groundOperationsCenter;
    private CheckInMediator checkInMediator;
    private BulkyBaggageDesk bulkyBaggageDesk;
    private SecurityMediator securityMediator;
    private ApronControl apronControl;
    private Tower tower;
    private AirportFuelTank fuelTank;
    private Customs customs;
    private BaggageSortingUnit baggageSortingUnit;

    // Airport Singleton
    private static Airport instance;
    private Airport() { // Prevent creation of airport by other classes
    	
    }
    public static synchronized Airport getInstance() {
    	if (Airport.instance == null) {
    		Airport.instance = new Airport();
    	}
    	return Airport.instance;
    }
    
    public void build() {
    	Airport airport = Airport.getInstance();
    	init(airport);
    }
    
    public void init(Airport airport) { // Create instances of classes
    	loadPassengerBaggageData(DATAFILEPATH.pathToString());
        resourcePool = new AirportResourcePool(50,50,50,50,50,50,50,50,50,50,50, airport);
        // TODO: Anzahlen anpassen, sobald kompilierbar
    	
        gateList = new ArrayList<Gate>(10);
        for(int number = 1; number <= 10; number++){
            Gate gate = new Gate(GATE_ID.getGateNumber(number), null);
            gateList.add(gate);
        }

        apronControl = new ApronControl();
        apronControl.setAirport(airport);
        apron = new Apron(airport, apronControl);
        apronControl.setApron(apron);

        groundOperationsCenter = new GroundOperationsCenter(airport, 100);

        bulkyBaggageDesk = new BulkyBaggageDesk(airport);

        checkInMediator = new CheckInMediator(bulkyBaggageDesk);

        FederalPolice police = new FederalPolice();
        securityMediator = new SecurityMediator(airport, police);

        WindDirectionSensor windDirectionSensor = new WindDirectionSensor();
        WindDirection windDirection = windDirectionSensor.measure();
        tower = new Tower(airport, null, windDirection);
        ArrayList<RunwayCheckPointID> runwayCheckpointIDR1 = new ArrayList<RunwayCheckPointID>();
    	runwayCheckpointIDR1.add(RunwayCheckPointID.S1);
    	runwayCheckpointIDR1.add(RunwayCheckPointID.S2);
    	ArrayList<RunwayCheckPointID> runwayCheckpointIDR2 = new ArrayList<RunwayCheckPointID>();
    	runwayCheckpointIDR2.add(RunwayCheckPointID.S3);
    	runwayCheckpointIDR2.add(RunwayCheckPointID.S4);
    	ArrayList<Runway> runwayList = new ArrayList<Runway>();
        if (windDirection == WindDirection.WestToEast) {
        	Runway r1 = new Runway(RunwayID.R08L, Position.North, runwayCheckpointIDR1, windDirectionSensor, false, false, null);
        	runwayList.add(r1);
        	Runway r2 = new Runway(RunwayID.R08R, Position.South, runwayCheckpointIDR2, windDirectionSensor, false, false, null);
        	runwayList.add(r2);
        } else {
        	// Different Runway IDs
        	Runway r1 = new Runway(RunwayID.R26R, Position.North, runwayCheckpointIDR1, windDirectionSensor, false, false, null);
        	runwayList.add(r1);
        	Runway r2 = new Runway(RunwayID.R26L, Position.South, runwayCheckpointIDR2, windDirectionSensor, false, false, null);
        	runwayList.add(r2);
        }
        IRunwayManagement runwayManagement = new RunwayManagement(null, runwayList, tower);
        tower.setRunwayManagement(runwayManagement);
        
        // TaxiWays
        ArrayList<TaxiCheckPoint> taxiCheckPointYellow = new ArrayList<TaxiCheckPoint>();
        taxiCheckPointYellow.add(TaxiCheckPoint.O1);
        taxiCheckPointYellow.add(TaxiCheckPoint.O2);
        taxiCheckPointYellow.add(TaxiCheckPoint.O3);
        taxiCheckPointYellow.add(TaxiCheckPoint.O4);
        taxiCheckPointYellow.add(TaxiCheckPoint.O5);
        taxiCheckPointYellow.add(TaxiCheckPoint.O6);
        TaxiWay taxiWayYellow = new TaxiWay(TaxiCenterLine.yellow, null, RunwayID.R26L, taxiCheckPointYellow, null);
        
        ArrayList<TaxiCheckPoint> taxiCheckPointGreen = new ArrayList<TaxiCheckPoint>();
        taxiCheckPointGreen.add(TaxiCheckPoint.N1);
        taxiCheckPointGreen.add(TaxiCheckPoint.N2);
        taxiCheckPointGreen.add(TaxiCheckPoint.N3);
        taxiCheckPointGreen.add(TaxiCheckPoint.N4);
        taxiCheckPointGreen.add(TaxiCheckPoint.N5);
        taxiCheckPointGreen.add(TaxiCheckPoint.N6);
        TaxiWay taxiWayGreen = new TaxiWay(TaxiCenterLine.green, null, RunwayID.R26R, taxiCheckPointGreen, null);
        
        ArrayList<TaxiCheckPoint> taxiCheckPointBlue = new ArrayList<TaxiCheckPoint>();
        taxiCheckPointBlue.add(TaxiCheckPoint.M1);
        taxiCheckPointBlue.add(TaxiCheckPoint.M2);
        taxiCheckPointBlue.add(TaxiCheckPoint.M3);
        taxiCheckPointBlue.add(TaxiCheckPoint.M4);
        taxiCheckPointBlue.add(TaxiCheckPoint.M5);
        taxiCheckPointBlue.add(TaxiCheckPoint.M6);
        TaxiWay taxiWayBlue = new TaxiWay(TaxiCenterLine.blue, null, RunwayID.R08L, taxiCheckPointBlue, null);
        
        ArrayList<TaxiCheckPoint> taxiCheckPointRed = new ArrayList<TaxiCheckPoint>();
        taxiCheckPointRed.add(TaxiCheckPoint.L1);
        taxiCheckPointRed.add(TaxiCheckPoint.L2);
        taxiCheckPointRed.add(TaxiCheckPoint.L3);
        taxiCheckPointRed.add(TaxiCheckPoint.L4);
        taxiCheckPointRed.add(TaxiCheckPoint.L5);
        taxiCheckPointRed.add(TaxiCheckPoint.L6);
        TaxiWay taxiWayRed = new TaxiWay(TaxiCenterLine.red, null, RunwayID.R08R, taxiCheckPointRed, null);
        
        fuelTank = new AirportFuelTank();

        customs = new Customs();
        BaggageScanner baggageScanner = new BaggageScanner(null, null);
        baggageSortingUnit = new BaggageSortingUnit(resourcePool.takeResource("Employee"), baggageScanner, null, customs, null);
        // Roboter wird bei Erstellung von baggageSortingUnit erstellt
    } 
    
    public int loadPassengerBaggageData(String dataFilePath){ //DATAFILEPATH.pathToString()
        PassengerBaggageDatabase passengerBaggageDatabase = new PassengerBaggageDatabase(dataFilePath);
        passengerList = passengerBaggageDatabase.getPassengerList();
        return passengerList.size();
    }

    public boolean connectAirplane(Airplane airplane, Gate gate){
        if(gate.getAirplane() == null) {
            gate.connect(airplane);
            return true;
        } else {
            System.out.println("Flugzeug kann nicht connected werden. Gate ist bereits belegt.");
            return false;
        }
    }

    public boolean disconnectAirplane(Airplane airplane, Gate gate){
        if ((gate.getAirplane() != null) && (gate.getAirplane() == airplane)) {
            gate.disconnectAirplane();
            return true;
        }
        else {
            System.out.println("Flugzeug kann nicht disconnected werden.");
            return false;}
    }

    public boolean executeServiceWasteWater(Gate gate){
        ServiceVehicleWasteWater serviceVehicle = resourcePool.takeResource("ServiceVehicleWasteWater");
        serviceVehicle.executeRequest(gate.getGateID());
        serviceVehicle.returnToAirportResourcePool();
        return true;
    }

    public boolean executeCheckIn(Flight flight){
        checkInMediator.executeRequest(flight);
        return true;
    }

    public boolean executeSecurity(){
        securityMediator.executeRequest();
        return true;
    }

    public boolean executeCustoms(){
        customs.executeRequest(baggageSortingUnit.getRoboter());
        return true;
    }

    public boolean executeAirCargo(GateID gateID){
        AirCargoPalletVehicle airCargoPalletVehicle = resourcePool.takeResource("AirCargoPalletVehicle");
        airCargoPalletVehicle.executeRequest(gateID);
        airCargoPalletVehicle.returnToAirportResourcePool();
        return true;
    }

    public boolean executeBaggageSortingUnit(GateID gateID){
        baggageSortingUnit.executeRequest(gateID);
        return true;
    }

    public boolean executeServiceBase(GateID gateID){
        ServiceVehicleBase base = resourcePool.takeResource("ServiceVehicleBase");
        base.executeRequest(gateID);
        base.returnToAirportResourcePool();
        return true;
    }

    public boolean executeServiceFreshWater(GateID gateID){
        ServiceVehicleFreshWater freshWaterVehicle = resourcePool.takeResource("ServiceVehicleFreshWater");
        freshWaterVehicle.executeRequest(gateID);
        freshWaterVehicle.returnToAirportResourcePool();
        return true;
    }

    public boolean executeServiceNitrogenOxygen(GateID gateID){
        ServiceVehicleNitrogenOxygen nitrogenOxygenVehicle = resourcePool.takeResource("ServiceVehicleNitrogenOxygen");
        nitrogenOxygenVehicle.executeRequest(gateID);
        nitrogenOxygenVehicle.returnToAirportResourcePool();
        return true;
    }

    public boolean executeSkyTanking(GateID gateID){
        SkyTankingVehicle skyTankingVehicle = resourcePool.takeResource("SkyTankingVehicle");
        skyTankingVehicle.executeRequest(gateID);
        resourcePool.returnResource(skyTankingVehicle);
        return true;
    }

    public boolean executeBoardingControl(Gate gate){
        securityMediator.executeRequest();
        return true;
    }

    public boolean executePushback(Gate gate){
        PushBackVehicle pushBackVehicle = resourcePool.takeResource("PushBackVehicle");
        TaxiWay taxiway = apronControl.search(TaxiCenterLine.yellow, gate.getGateID(), RunwayID.R26L);
        // TODO: Übergabeparameter korrekt?
        pushBackVehicle.execute(gate.getAirplane(), taxiway);
        resourcePool.returnResource(pushBackVehicle);
        return true;
    }

    public boolean executeGroundOperationsLogging(){
       groundOperationsCenter.logBaggageSortingUnit(groundOperationsCenter.getBaggageSortingUnitReceiptList());
       groundOperationsCenter.logBoardingControl(groundOperationsCenter.getBoardingControlReceiptList());
       groundOperationsCenter.logBulkyBaggageDesk(groundOperationsCenter.getBulkyBaggageDeskReceiptList());
       groundOperationsCenter.logCargoPalletLifter(groundOperationsCenter.getAirCargoPalletLifterReceiptList());
       groundOperationsCenter.logCheckIn(groundOperationsCenter.getCheckInReceiptList());
       groundOperationsCenter.logContainerLifter(groundOperationsCenter.getContainerLifterReceiptList());
       groundOperationsCenter.logCustoms(groundOperationsCenter.getCustomsReceiptList());
       groundOperationsCenter.logFederalPolice(groundOperationsCenter.getFederalPoliceReceiptList());
       groundOperationsCenter.logFuel(groundOperationsCenter.getFuelReceiptList());
       groundOperationsCenter.logPushbackVehicle(groundOperationsCenter.getPushBackVehicleReceiptList());
       groundOperationsCenter.logServiceVehicleBase(groundOperationsCenter.getServiceVehicleBaseReceiptList());
       groundOperationsCenter.logServiceVehicleFreshWater(groundOperationsCenter.getServiceVehicleFreshWaterReceiptList());
       groundOperationsCenter.logSecurityCheck(groundOperationsCenter.getSecurityCheckReceiptList());
       groundOperationsCenter.logServiceVehicleWasteWater(groundOperationsCenter.getServiceVehicleWasteWaterReceiptList());
       groundOperationsCenter.logServiceVehicleNitrogenOxygen(groundOperationsCenter.getServiceVehicleNitrogenOxygenReceiptList());
        return true;
    }

    //
    // Getter und Setter
    //

    public AirportFuelTank getFuelTank(){
        return this.fuelTank;
    }

    public AirportResourcePool getResourcePool(){
        return this.resourcePool;
    }

    public CheckInMediator getCheckInMediator() {
        return checkInMediator;
    }

    public GroundOperationsCenter getGroundOperationsCenter() {
        return groundOperationsCenter;
    }

    public ArrayList<Passenger> getPassengerList() {
        return passengerList;
    }

    public ArrayList<Gate> getGateList() {
        return this.gateList;
    }

    public BaggageSortingUnit getBaggageSortingUnit(){
        return this.baggageSortingUnit;
    }

    ///
    /// Gate von GateID
    ///

    public Gate getGatefromID(GateID gateid){
        for(Gate gate: gateList){
            if(gate.getGateID() == gateid){
                return gate;
            }
        }
        return null;
    }

}