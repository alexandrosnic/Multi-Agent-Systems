package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;

import java.util.*;

/**
 * A DeliveryAgent Created in 2018 for the 2018 City scenario.
 */
public class DeliveryAgent extends Agent{

    private State state = State.EXPLORE;
    private Percept goal;

    private String role = "";
    private int charge = 0;
    private int battery = 0;
    private double lat = 0;
    private double lon = 0;
    private double minLat = 0;
    private double maxLat = 0;
    private double minLon = 0;
    private double maxLon = 0;
    private String lastAction = "";
    private String lastActionResult = "";
    private int massium = 0;
    private int score = 0;
    private int step = 0;

    private Map<String, Percept> resourceNodes = new HashMap<>();
    private Map<String, Percept> storageGlobal = new HashMap<>();
    private Set<String> availableResources = new HashSet<>();
    private String wellName;
    private int wellCost = Integer.MAX_VALUE;
    private Map<String, Percept> chargingStations = new HashMap<>();

    private String leader = "";
    private Loc exploreTarget;
    private Loc chargingTarget;
    private Loc resourceNodeTarget;
    private int buildCounter;
    private int lastBuild = -100;
    private String currentJob = "";
    private Map<String, Percept> jobs = new HashMap<>();

    private Random rand = new Random(17);

	//***************      What I added:	***************
	private String charge_truck = "";
	private String carriedItems = "";
	private int numItems = 0;
	private Map<String, Percept> shopsPercept = new HashMap<>();
	private Map<String, Percept> dumpsPercept = new HashMap<>();
	private String myJob;
	private Set<String> jobsTaken = new HashSet<>();
	private Queue<Action> actionQueue = new LinkedList<>();
	private Loc dumpLoc;
	private Map<String, Percept> currentJobsGlobal = new HashMap<>();
	private Map<String, List<Percept>> shopsByItemGlobal = new HashMap<>();
	private Loc shopLoc;
	private Loc storageLoc;
	private Map<String, Percept> shopsGlobal = new HashMap<>();
	private Map<String, Percept> agentsGlobal = new HashMap<>();
	private Map<String, Percept> dronesGlobal = new HashMap<>();
	private String itemNameGlobal;
	private int amountGlobal;
    private boolean flagCharged = true;
    private boolean busy;
    private Percept currentItem;
	private String currentJob_truck = "";
	private Map<String, Percept> DeliveryTrucks = new HashMap<>();
	private Loc truckTarget;
	private String receivedItem;
	private boolean dumpItem;
	String truckLoc = "";
	private String roleEntity = "";
	private Map<String, Percept> trucksLocal = new HashMap<>();
	private Map<String, Percept> trucksGlobal = new HashMap<>();
	private boolean moveTruck = true;
	//private ParameterList theTruckName;
	// ****************************************************


    /**
     * Constructor
     *
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public DeliveryAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public Action step() {

		Map<String, Percept> trucksGlobal = new HashMap<>();
		Map<String, List<Percept>> shopsByItem = new HashMap<>();
		Map<String, Percept> currentJobs = new HashMap<>();

        // ********************		PERCEPTIONS		******************** //
        List<Percept> percepts = getPercepts();
		say("The percepts are: " + getPercepts());
        for (Percept percept : percepts) {
            switch(percept.getName()) {
                case "job":
                    jobs.put(getStringParam(percept, 0), percept);
                    break;
                case "resourceNode":
                    String resourceNodeName = getStringParam(percept, 0);
                    broadcast(percept, getName());
                    resourceNodes.put(resourceNodeName, percept);
                    availableResources.add(getStringParam(percept, 3));
                    say("Found a resource node of name " + resourceNodeName);
                    break;
                case "storage":
                    storageGlobal.put(getStringParam(percept, 0), percept);
                    break;
                case "role":
                    role = getStringParam(percept, 0);
                    say("Role: " + role);
                    battery = getIntParam(percept, 9);
                    break;
                case "charge":
                    charge = getIntParam(percept, 0);
                    break;
                case "minLat": minLat = getDoubleParam(percept, 0); break;
                case "maxLat": maxLat = getDoubleParam(percept, 0); break;
                case "minLon": minLon = getDoubleParam(percept, 0); break;
                case "maxLon": maxLon = getDoubleParam(percept, 0); break;
                case "lat": lat = getDoubleParam(percept, 0); break;
                case "lon": lon = getDoubleParam(percept, 0); break;
                case "lastAction": lastAction = getStringParam(percept, 0); break;
                case "lastActionResult": lastActionResult = getStringParam(percept, 0); break;
                case "wellType":
                    int cost = getIntParam(percept, 1);
                    if(cost < wellCost) {
                        wellCost = cost;
                        wellName = getStringParam(percept, 0);
                    }
                    break;
                case "massium": massium = getIntParam(percept, 0); break;
                case "score": score = getIntParam(percept, 0); break;

				case "dump": dumpsPercept.put(getStringParam(percept, 0), percept); break;
                case "chargingStation": chargingStations.put(getStringParam(percept, 0), percept); break;
                case "step": step = getIntParam(percept, 0); break;
				case "item":
                    carriedItems += " " + percept.toProlog();
                    currentItem = percept;
					numItems += 1;
                    break;
				case "entity":
					
					//entities = percept.getParameters();
					say("The entities are: "+ percept);
				    roleEntity = getStringParam(percept, 4);
					broadcast(percept, getName());
					if(roleEntity.equals("drone")) {
						dronesGlobal.put(getStringParam(percept, 0), percept); 
                    } else if (roleEntity.equals("truck")) {
						trucksGlobal.put(getStringParam(percept, 0), percept); 
						trucksLocal = trucksGlobal;
						//theTruckName.add(percept.getParameters().get(0));
					}
					agentsGlobal.put(getStringParam(percept, 0), percept); 
					say("My trucks of the game are: " + trucksGlobal);
                    break;
			}
			if(actionQueue.size() == 0){
                // parse info needed for planning
                switch(percept.getName()){
					case "job":
						// remember all active jobs
						currentJobs.putIfAbsent(stringParam(percept.getParameters(), 0), percept);
						currentJobsGlobal = currentJobs;
						break;
					case "shop":
						// remember shops by what they offer
						ParameterList stockedItems = listParam(percept, 4);
						for(Parameter stock: stockedItems){
							if(stock instanceof Function){
								String itemName = stringParam(((Function) stock).getParameters(), 0);
								int amount = intParam(((Function) stock).getParameters(), 2);
								if(amount > 0){
									shopsByItem.putIfAbsent(itemName, new ArrayList<>());
									shopsByItem.get(itemName).add(percept);
									shopsByItemGlobal = shopsByItem;
								}
							}
						}
						break;
				}
            }
        
        }

        // "elect" a leader
        if(!role.equalsIgnoreCase("drone") && leader.equals("")) {
            broadcast(new Percept("leader"), getName());
            this.leader = getName();
        }

		// "elect" the charging truck
        if(!role.equalsIgnoreCase("truck") && charge_truck.equals("")) {
            broadcast(new Percept("charge_truck"), getName());
            this.charge_truck = getName();
        }

        say("My last action was " + lastAction + " : " + lastActionResult);

        if(leader.equals(getName())) say("Score: " + score + " Massium: " + massium);

		return act();
	}



		// ********************		ACTIONS		******************** //

	private Action act(){
        
        // if(getName().equals(leader) && currentJob.equals("")) {
        //     for(Percept job: jobs.values()) {
        //         int endStep = getIntParam(job, 4);
        //         if((endStep - step) > 100) {
        //             ParameterList items = (ParameterList) job.getParameters().get(5);
        //             // TODO distribute items among team members
        //         }
        //     }
        // }


		// if(getName().equals(charge_truck) && currentJob_truck.equals("")) {
        //     for(Percept job: jobs.values()) {
        //         int endStep = getIntParam(job, 4);
        //         if((endStep - step) > 100) {
        //             ParameterList items = (ParameterList) job.getParameters().get(5);
        //             // TODO distribute items among team members
        //         }
        //     }
        // }
        
        if(role.equals("drone") ) {

			if(state == State.BATTERY) {
				
					Random randNum = new Random();
					double charge_or_truck = randNum.nextDouble();
					say("charge_or_truck is: " + charge_or_truck);
					String station = "";
					double minDist = Double.MAX_VALUE;
					for(Percept p: chargingStations.values()) {
						double cLat = getDoubleParam(p, 1);
						double cLon = getDoubleParam(p, 2);
						double dist = Math.sqrt(Math.pow(lat - cLat, 2) + Math.pow(lon - cLon, 2));
						if(dist < minDist) {
							minDist = dist;
							station = getStringParam(p, 0);
						}
					}
					if(!station.equals("")) {
						Percept p = chargingStations.get(station);
						chargingTarget = new Loc(getDoubleParam(p, 1), getDoubleParam(p, 2));
					}
					if (charge_or_truck > 0.5) {
						state = State.RECHARGE;

						flagCharged = false;
					}
					else {
						if(trucksGlobal.isEmpty()) {
							state = State.RECHARGE;
						}
						else {
							state = State.GIVE_AMAZON_ALEXA;
						
							double minDistTruck = Double.MAX_VALUE;
							for(Percept p: trucksGlobal.values()) {
								double tLat = getDoubleParam(p, 2);
								double tLon = getDoubleParam(p, 3);
								double distTruck = Math.sqrt(Math.pow(lat - tLat, 2) + Math.pow(lon - tLon, 2));
								if(distTruck < minDistTruck) {
									minDistTruck = distTruck;
									truckLoc = getStringParam(p, 0);
									say("the trucks here are: " + trucksGlobal);
									say("and the truck name is : " + truckLoc);
								}
							}
							if(!truckLoc.equals("")) {
								Percept p = trucksGlobal.get(truckLoc);
								say("and the truck name now is : " + truckLoc);
								say("the percept i chose : " + p);
								say("the trucksGlobal i chose : " + trucksGlobal);
								say("the percept i chose manually : " + trucksGlobal.get(truckLoc));
								truckTarget = new Loc(getDoubleParam(p, 2), getDoubleParam(p, 3));
							}
						}
						flagCharged = false;
					}

			}
    
            if(state == State.RECHARGE) {
                // ADD SOMETHING IN GOAL!!!
                if(charge > .8 * battery) {
                    // TODO resume goal
					state = State.JOB;
                }
                else {
                    if (chargingTarget != null) {
                        if (atLoc(chargingTarget)) {
                            flagCharged = true;
                            say("Let's charge the battery");
                            // Random giveItem = new Random();
                            // int random = randTruck.nextDouble(1);
                            if (currentItem != null) {
                                // state = State.GIVE_AMAZON_ALEXA;
                            }
							state = State.EXPLORE;
                            return new Action("charge");
                        }
                        else {
                            say("Let's go to the charging station");
                            return new Action("goto", 
                            new Numeral(chargingTarget.lat), new Numeral(chargingTarget.lon));
                        }
                    }
                }
            }
    
            if(exploreTarget != null) {
                if(atLoc(exploreTarget)) {
                    // target reached
                    exploreTarget = null;
                }
            }
    
            if(state == State.EXPLORE) {
                say("I'm exploring!!");
				if(charge < .6 * battery) {
					state = State.BATTERY;
				}
                if(exploreTarget == null || lastActionResult.equalsIgnoreCase("failed_no_route")) {
                    double expLat = minLat + rand.nextDouble() * (maxLat - minLat);
                    double expLon = minLon + rand.nextDouble() * (maxLon - minLon);
                    exploreTarget = new Loc(expLat, expLon);
                    busy = false;
                }
                return new Action("goto", new Numeral(exploreTarget.lat), new Numeral(exploreTarget.lon));
            }

            if(state == State.GIVE_AMAZON_ALEXA){
				Percept theTruck = trucksGlobal.get(truckLoc);
				if (atLoc(truckTarget)) {
					state = State.RECHARGE;
					say("I arrived at the truck");
					receivedItem = "true";
					broadcast(new Percept("receivedItem", new Identifier(receivedItem)), getName());
					// TO FIX
					Parameter currentItemName = currentItem.getParameters().get(0);
					Parameter theTruckNameLocal = theTruck.getParameters().get(0);
					return new Action("give", theTruckNameLocal, currentItemName);
				}
				else {
				    say("I'm running out of battery. I have to give the parcel to the delivery truck");
                    return new Action("goto", 
                    new Numeral(truckTarget.lat), new Numeral(truckTarget.lon));
				}

            }

            if(!busy && !resourceNodes.isEmpty()){
                double minDist = Double.MAX_VALUE;
                String resource = "";
                for(Percept p: resourceNodes.values()){
                    say("Resource node: " + p);
                    double cLat = getDoubleParam(p, 1);
                    double cLon = getDoubleParam(p, 2);
                    double dist = Math.sqrt(Math.pow(lat - cLat, 2) + Math.pow(lon - cLon, 2));
                    if(dist < minDist) {
                        minDist = dist;
                        resource = getStringParam(p, 0);
                    }
                }
                if(!resource.equals("")) {
                    Percept p = resourceNodes.get(resource);
                    resourceNodeTarget = new Loc(getDoubleParam(p, 1), getDoubleParam(p, 2));
                    state = State.GATHER;
                }
				else {
					state = State.EXPLORE;
				}
            }

            if(state == State.GATHER) {
                // ADD SOMETHING IN GOAL!!!
                say("Entering the gather state");
                if (resourceNodeTarget != null) {
                    if (atLoc(resourceNodeTarget)) {
                        say("Let's gather the item and change state to JOB");
                        state = State.JOB;
                        return new Action("gather");
                    }
                    else {
                        busy = true;
                        say("Let's go to the resource node location to get the item");
                        return new Action("goto", 
                        new Numeral(resourceNodeTarget.lat), new Numeral(resourceNodeTarget.lon));
                    }
                }
            }

            // Associate a job to the agent
            if (myJob == null){
                
                Set<String> availableJobs = new HashSet<>(currentJobsGlobal.keySet());
                availableJobs.removeAll(jobsTaken);
                if(availableJobs.size() > 0){
                    myJob = availableJobs.iterator().next();
                    say("I will complete " + myJob);
                    jobsTaken.add(myJob);
                    broadcast(new Percept("taken", new Identifier(myJob)), getName());
                }
            }

            // If already associated a job, carry it out
            if(state == State.JOB){
                if(myJob != null){
                    // plan the job
                    // 1. acquire items
                    Percept job = currentJobsGlobal.get(myJob);
                    if(job == null){
                        say("I lost my job :(");
                        myJob = null;
                        return new Action("skip");
                    }
                    String storage = stringParam(job.getParameters(), 1);
                    ParameterList requirements = listParam(job, 5);
                    
                    for (Percept pStorage: storageGlobal.values()) {
                        String storageName = stringParam(pStorage.getParameters(), 0);
                        if(storageName.equalsIgnoreCase(storage)){
                            storageLoc = new Loc(getDoubleParam(pStorage, 1), getDoubleParam(pStorage, 2));
                            break;
                        }
                    }
                    say("The storage to go to: " + storage +  " -> Location: " +  storageLoc);
                    if(storageLoc != null){
                        if(atLoc(storageLoc)){
                            state = State.STORE;
                        }
                        else{
                            return new Action("goto", new Identifier(storage));
                        }
                    }
                }
            }
            if(state == State.SHOP) {
                // go to the shop
                Percept shop = shopsGlobal.get(0);
                shopLoc = new Loc(getDoubleParam(shop, 1), getDoubleParam(shop, 2));
                if(atLoc(shopLoc)){
                    state = State.BUY;
                }
                return new Action("goto", new Identifier(stringParam(shop.getParameters(), 0)));
            }
            if(state == State.BUY) {
                state = State.STORAGE;
                // buy the items
                return new Action("buy", new Identifier(itemNameGlobal), new Numeral(amountGlobal));
            }
            
            if(state == State.STORE) {
                state = State.DELIVERED;
                // 2.1 store items
                say("Storing item");
                return new Action("store");
            }
            if(state == State.DELIVERED) {
                state = State.EXPLORE;
                // 2.2 deliver items
                say("Set the item as delivered and going to explore");
                return new Action("deliver_job", new Identifier(myJob));
            }

            return new Action("continue");
        }
        else if(role.equals("truck")){
            if (receivedItem=="true"){
				receivedItem = "false";
				dumpItem = true;
				state = State.DELIVERTRUCK;
                return new Action("receive");
            }
			if (state == State.DELIVERTRUCK){
				
				String myDump = "";
				for(Percept p: dumpsPercept.values()) {
					myDump = getStringParam(p, 0);
				}

				Percept p = dumpsPercept.get(myDump);
				dumpLoc = new Loc(getDoubleParam(p, 1), getDoubleParam(p, 2));
				if (dumpLoc != null) {
					if(atLoc(dumpLoc)){
					// TO FIX
						Parameter currentItemName = currentItem.getParameters().get(0);
						dumpItem = false;
						state = State.EXPLORE;
						return new Action("dump", currentItemName);
					} else {

						return new Action("goto", new Numeral(dumpLoc.lat), new Numeral(dumpLoc.lon));
					}
				}
			}



			if(charge < .5 * battery) {
				state = State.RECHARGE;
				String station = "";
				double minDist = Double.MAX_VALUE;
				for(Percept p: chargingStations.values()) {
					double cLat = getDoubleParam(p, 1);
					double cLon = getDoubleParam(p, 2);
					double dist = Math.sqrt(Math.pow(lat - cLat, 2) + Math.pow(lon - cLon, 2));
					if(dist < minDist) {
						minDist = dist;
						station = getStringParam(p, 0);
					}
				}
				if(!station.equals("")) {
					Percept p = chargingStations.get(station);
					chargingTarget = new Loc(getDoubleParam(p, 1), getDoubleParam(p, 2));
				}
			}

			if(state == State.RECHARGE) {
				if(charge > .8 * battery) {
					if(goal != null) {
						// TODO resume goal
					}
					else {
						state = State.EXPLORE;
					}
				}
				else {
					if (chargingTarget != null) {
						if (atLoc(chargingTarget)) {
							return new Action("charge");
						}
						else {
							return new Action("goto", new Numeral(chargingTarget.lat), new Numeral(chargingTarget.lon));
						}
					}
				}
			}
			if(exploreTarget != null) {
				if(atLoc(exploreTarget)) {
					// target reached
					moveTruck = false;
					exploreTarget = null;
				}
			}

			if(state == State.EXPLORE) {
				if(exploreTarget == null || lastActionResult.equalsIgnoreCase("failed_no_route")) {
					double expLat = minLat + rand.nextDouble() * (maxLat - minLat);
					double expLon = minLon + rand.nextDouble() * (maxLon - minLon);
					exploreTarget = new Loc(expLat, expLon);
				}
				return new Action("goto", new Numeral(exploreTarget.lat), new Numeral(exploreTarget.lon));
			}


			
            
        }
		return new Action("continue");
    }

    private boolean atLoc(Loc loc) {
        return Math.abs(lat - loc.lat) < .0001 && Math.abs(lon - loc.lon) < .0001;
    }

    private String getStringParam(Percept percept, int position) {
        Parameter p = percept.getParameters().get(position);
        if (p instanceof Identifier) return ((Identifier) p).getValue();
        return "";
    }

    private int getIntParam(Percept percept, int position) {
        Parameter p = percept.getParameters().get(position);
        if (p instanceof Numeral) return ((Numeral) p).getValue().intValue();
        return 0;
    }

    private double getDoubleParam(Percept percept, int position) {
        Parameter p = percept.getParameters().get(position);
        if (p instanceof Numeral) return ((Numeral) p).getValue().doubleValue();
        return 0;
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        switch(message.getName()) {
            case "leader":
                this.leader = sender;
                say("I agree to " + sender + " being the group leader.");
                break;
            case "resourceNode":
                String name = ((Identifier)message.getParameters().get(0)).getValue();
                String resource = ((Identifier)message.getParameters().get(3)).getValue();
                resourceNodes.put(name, message);
                availableResources.add(resource);
                break;
			case "taken":
                jobsTaken.add(stringParam(message.getParameters(), 0));
                break;
			case "receivedItem":
				say("message.getParameters().get(0) = "+ message.getParameters().get(0));
				receivedItem = stringParam(message.getParameters(), 0);
				break;
			case "entity":
				String roleAgent = ((Identifier)message.getParameters().get(4)).getValue();
				String nameAgent = ((Identifier)message.getParameters().get(0)).getValue();
				if(roleAgent.equals("drone")) {
					dronesGlobal.put(nameAgent, message); 
				} else if (roleAgent.equals("truck")) {
					trucksGlobal.put(nameAgent, message); 
					//theTruckName.add(nameAgent);
				}
				say("TRUUUUUUUUUUUCKS" + trucksGlobal);
				break;
            default:
                say("I cannot handle a message of type " + message.getName());
        }
    }

    class Loc {
        double lat;
        double lon;
        Loc(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    enum State {
        EXPLORE, RECHARGE, JOB, STORE, DELIVERED, STORAGE, SHOP, BUY, GATHER,
        GIVE_AMAZON_ALEXA, BATTERY, DELIVERTRUCK
    }

    private static ParameterList listParam(Percept p, int index){
        List<Parameter> params = p.getParameters();
        if(params.size() < index + 1) return new ParameterList();
        Parameter param = params.get(index);
        if(param instanceof ParameterList) return (ParameterList) param;
        return new ParameterList();
    }

    public static String stringParam(List<Parameter> params, int index){
        if(params.size() < index + 1) return "";
        Parameter param = params.get(index);
        if(param instanceof Identifier) return ((Identifier) param).getValue();
        return "";
    }

    private static int intParam(List<Parameter> params, int index){
        if(params.size() < index + 1) return -1;
        Parameter param = params.get(index);
        if(param instanceof Numeral) return ((Numeral) param).getValue().intValue();
        return -1;
    }
}
