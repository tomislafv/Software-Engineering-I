package Airport.Baggage_Sorting_Unit;

import Airport.Airport.Airport;
import Airport.Airport.Gate;
import Airport.Airport.GateID;
import Airport.Baggage_Sorting_Unit.Loading.AirplaneLoadingManagement;
import Airport.Baggage_Sorting_Unit.Loading.LoadingStrategy;
import Airport.Baggage_Sorting_Unit.Receipts.BaggageSortingUnitReceipt;
import Airport.Baggage_Sorting_Unit.Receipts.ContainerLifterReceipt;
import Airport.Baggage_Sorting_Unit.Storage.BaggageDepot;
import Airport.Baggage_Sorting_Unit.Storage.BaggageSortingUnitRoboter;
import Airport.Baggage_Sorting_Unit.Storage.IBaggageSortingUnitRoboter;
import Airport.Baggage_Sorting_Unit.Vehicles.ContainerLifter;
import Airport.Baggage_Sorting_Unit.Vehicles.IBaggageVehicle;
import Airport.Base.*;
import Airport.Customs.ICustoms;
import Airport.Scanner.BaggageScanner;
import Airport.Scanner.IBaggageScanner;

import java.util.ArrayList;
import java.util.Arrays;

public class BaggageSortingUnit implements IBaggageSortingUnit {

    private final ArrayList<Employee> employeeList;

    private ArrayList<String> scanPatternList;


    private final IBaggageScanner baggageScanner;

    private IBaggageSortingUnitRoboter roboter;

    private final BaggageDepot baggageDepot;

    private final DestinationBox destinationBox;

    private final ArrayList<LuggageTub> emptyLuggageTubList;

    private final ArrayList<Container> emptyContainerList;

    private final ArrayList<Container> filledContainerList;

    private final ContainerLifter containerLifter;

    private IBaggageVehicle baggageVehicle;

    private final ICustoms customs;

    protected final Airport airport;

    private Gate gate;

    /**
     * Init
     * TODO: set correct initial values
     */
    public BaggageSortingUnit(final ArrayList<Employee> employeeList,
                              final BaggageScanner baggageScanner, final DestinationBox destinationBox,
                              final ICustoms customs) {

        this.employeeList = employeeList;
        this.baggageScanner = baggageScanner;
        roboter = new BaggageSortingUnitRoboter(this, "42");//TODO init correctly
        baggageDepot = new BaggageDepot();
        this.destinationBox = destinationBox;
        emptyLuggageTubList = new ArrayList<>();
        emptyContainerList = new ArrayList<>();
        filledContainerList = new ArrayList<>();
        this.customs = customs;
        airport = Airport.getInstance();
        scanPatternList = new ArrayList<>(Arrays.asList("drugs", "glock7", "exp!os!ve", "knife"));
        containerLifter = airport.getResourcePool().takeResource("ContainerLifter");
    }

    @Override
    public String toString() {
        final String message = "Employees: " + employeeList + "\nScanner: " + baggageScanner
                + "\nRoboter: " + roboter + "\nDepot: " + baggageDepot + "\nDestination box: "
                + destinationBox + "\nFilled containers: " + filledContainerList;

        return message;
    }

    public Gate getGate() {
        return gate;
    }

    public void setGate(final Gate gate) {
        this.gate = gate;
    }

    public ArrayList<Employee> getEmployeeList() {
        return employeeList;
    }

    public ArrayList<String> getScanPatternList() {
        return scanPatternList;
    }

    public BaggageDepot getBaggageDepot() {
        return baggageDepot;
    }

    public ArrayList<LuggageTub> getEmptyLuggageTubList() {
        return emptyLuggageTubList;
    }

    public ArrayList<Container> getEmptyContainerList() {
        return emptyContainerList;
    }

    public ArrayList<Container> getFilledContainerList() {
        return filledContainerList;
    }

    public IBaggageSortingUnitRoboter getRoboter() {
        return roboter;
    }

    public DestinationBox getDestinationBox() {
        return destinationBox;
    }

    @Override
    public IBaggageVehicle getVehicle() {
        return baggageVehicle;
    }

    @Override
    public BaggageDepot getDepot() {
        return baggageDepot;
    }

    /**
     * TODO: check Kickoff routine
     * <p>
     * kicks of the routine for sorting all Baggage from the given Gate
     */
    @Override
    public void executeRequest(final GateID gateID) {
        setGate(gateID);
        final ArrayList<LuggageTub> fullTubs = new ArrayList<>();//TODO get list of full tubs from checkin (CheckInDesk.getLuggageTubList())
        loginBaggageScanner(employeeList.get(0),
                "420");//TODO get correct user and pw employeeList.get(0).getPassword()
        LuggageTub l;
        while (!fullTubs.isEmpty()) {
            l = fullTubs.remove(0);
            final Baggage toCheck = l.getBaggage();
            toCheck.setSecurityStatus(BaggageSecurityStatus.clean);
            for (String pattern : scanPatternList) {
                boolean clean = scan(toCheck, pattern); //TODO check how scan is implemented
                if (!clean) {
                    toCheck.setSecurityStatus(BaggageSecurityStatus.dangerous);
                }

            }
            if (toCheck.getSecurityStatus() == BaggageSecurityStatus.clean) {
                throwOff(l, destinationBox);
            } else {
                handOverToCustoms(l.getBaggage());
                emptyLuggageTubList.add(l);
                l.setBaggage(null);
            }
            if (destinationBox.isFull()) {
                emptyDestinationBox();
            }
        }
        logoutBaggageScanner();

        if (!destinationBox.isempty()) {
            emptyDestinationBox();
        }

        roboter.selectBaggageFromDepot();

        sendBaggageVehicleToGate();

        sendContainerLifterToGate();

        baggageVehicle.connect(containerLifter);

        loadBaggageVehicle(optimizeAirplaneLoading());

        baggageVehicle.disconnect();

        baggageVehicle.returnToBaggageSortingUnit();

        containerLifter.disconnectFromAirplane();

        containerLifter.notifyGroundOperations(new ContainerLifterReceipt(containerLifter.getId(), containerLifter.getGate().getGateID(), containerLifter.getNumberOfContainerLoaded(), containerLifter.getContainerIDList()));

        containerLifter.returnToAirportResourcePool();

        //notifyGroundOperations(new BaggageSortingUnitReceipt()); TODO generate receipt
    }

    /**
     * logs employee into Scanner via iDCard
     */
    @Override
    public void loginBaggageScanner(final Employee employee, final String password) {
        baggageScanner.login(new IDCard(), password);//TODO employee.getIdCard()
    }

    /**
     * logs user out from Scanner
     */
    @Override
    public void logoutBaggageScanner() {
        baggageScanner.logout();
    }

    /**
     * calls scan of baggageScanner
     */
    @Override
    public boolean scan(final Baggage baggage, final String pattern) {
        return baggageScanner.scan(baggage, pattern);
    }

    /**
     * adds baggage to roboter list and passes roboter to customs, clears roboter
     */
    @Override
    public void handOverToCustoms(final Baggage baggage) {//TODO take Methode von customs officer woher?
    }

    /**
     * checks baggage and sets securityStatus
     * adds it to destinationBox or hands over to customs
     */
    @Override
    public void throwOff(final LuggageTub luggageTub, final DestinationBox destinationBox) {
        destinationBox.addBagagge(luggageTub.getBaggage());
        luggageTub.setBaggage(null);
        emptyLuggageTubList.add(luggageTub);
    }

    /**
     * Tells Robot to move all baggage to the depot and clears local box
     */
    @Override
    public void emptyDestinationBox() {
        roboter.moveBaggageToDepot(destinationBox.getBaggageList());
        destinationBox.empty();
    }

    /**
     * get lifter from baggageVehicle move it and set the gate
     */
    @Override
    public void sendContainerLifterToGate() {
        containerLifter.executeRequest(gate.getGateID());
    }

    /**
     * Creates AirplaneLoadingManagement to balance a strategy
     */
    @Override
    public LoadingStrategy optimizeAirplaneLoading() {
        final AirplaneLoadingManagement manage
                = new AirplaneLoadingManagement(filledContainerList);
        manage.optimizeBalancing();
        return manage.getStrategy();
    }

    @Override
    public void setBaggageVehicle(final IBaggageVehicle vehicle) {
        baggageVehicle = vehicle;
    }

    /**
     * Tells robot to select Baggage and load the container
     */
    @Override
    public void loadBaggageVehicle(final LoadingStrategy strategy) {

        for (String id : strategy.getContainerIDList()) {
            baggageVehicle.store(getContainerByID(id));
            baggageVehicle.transferContainerToLifter();
            containerLifter.transferContainerToCargoSystem();
        }
    }

    private Container getContainerByID(String id) {
        for (Container c : filledContainerList) {
            if (c.getId().equals(id)) {
                filledContainerList.remove(c);
                return c;
            }
        }
        return null;
    }

    /**
     * Moves vehicle and sets gate
     */
    @Override
    public void sendBaggageVehicleToGate() {
        baggageVehicle.executeRequest(gate.getGateID());
    }

    /**
     * gets GroundoperationsCenter and notifys it with the receipt
     */
    @Override
    public void notifyGroundOperations(final BaggageSortingUnitReceipt baggageSortingUnitReceipt) {
        Airport.getInstance().getGroundOperationsCenter().receive(baggageSortingUnitReceipt);
    }

    /**
     * Returns empty luggage tubs via CheckInMediator
     */
    @Override
    public void returnEmptyLuggageTubToCheckInDesk() {
        //airport.getCheckInMediator().returnLuggageTubs(emptyLuggageTubList); TODO Get Mediator from airport
        emptyLuggageTubList.clear();
    }

    public Container getEmptyContainer(ContainerCategory containerCategory) {
        for (Container c : emptyContainerList) {
            if (c.getCategory() == containerCategory) {
                emptyContainerList.remove(c);
                return c;
            }
        }
        return null;
    }

    public void addFullContainer(Container container) {
        filledContainerList.add(container);
    }

    /**
     * finds correct gate for the ID and sets internal attribute
     */
    private void setGate(final GateID id) {
        gate = Airport.getInstance().getGatefromID(id);
    }
}
