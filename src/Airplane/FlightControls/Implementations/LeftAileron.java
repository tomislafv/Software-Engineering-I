package Airplane.FlightControls.Implementations;

import Airplane.FlightControls.AbstractClasses.VerticalRotable;
import Airplane.FlightControls.Interfaces.ILeftAileron;

class LeftAileron extends VerticalRotable implements ILeftAileron {

    public LeftAileron(String manufacturer, String type) {
        super(manufacturer, type, 45, 0);
        this.degree = 0;
    }
}
