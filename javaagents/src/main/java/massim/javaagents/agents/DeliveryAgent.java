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
    private Set<String> availableResources = new HashSet<>();
    private String wellName;
    private int wellCost = Integer.MAX_VALUE;
    private Map<String, Percept> chargingStations = new HashMap<>();

    private String leader = "";
    private Loc exploreTarget;
    private Loc chargingTarget;
    private int buildCounter;
    private int lastBuild = -100;
    private String currentJob = "";
    private Map<String, Percept> jobs = new HashMap<>();

    private Random rand = new Random(17);

	//***************      What I added:	***************
	String carriedItems = "";
	int numItems = 0;
	private Map<String, Percept> shopsPercept = new HashMap<>();
	private Map<String, Percept> dumpsPercept = new HashMap<>();
	private String myJob;
	private Set<String> jobsTaken = new HashSet<>();
	private Queue<Action> actionQueue = new LinkedList<>();
	private Loc dumpLoc;
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

		
		Map<String, List<Percept>> shopsByItem = new HashMap<>();
		Map<String, Percept> currentJobs = new HashMap<>();

        // ********************		PERCEPTIONS		******************** //
        List<Percept> percepts = getPercepts();
        for (Percept percept : percepts) {
            switch(percept.getName()) {
                case "job":
                    jobs.put(getStringParam(percept, 0), percept);
                    break;
                case "resourceNode":
                    broadcast(percept, getName());
                    resourceNodes.put(getStringParam(percept, 0), percept);
                    availableResources.add(getStringParam(percept, 3));
                    say("Found a resource node.");
                    break;
                case "role":
                    role = getStringParam(percept, 0);
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
					numItems += 1;
                    break;
			}
			if(actionQueue.size() == 0){
                // parse info needed for planning
                switch(percept.getName()){
					case "job":
						// remember all active jobs
						currentJobs.putIfAbsent(stringParam(percept.getParameters(), 0), percept);
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

        say("My last action was " + lastAction + " : " + lastActionResult);

        if(leader.equals(getName())) say("Score: " + score + " Massium: " + massium);

        /*
		if(carriedItems.isEmpty()==false){
            say("I carry " + carriedItems);
        }
        */



		// ********************		ACTIONS		******************** //

        if(getName().equals(leader) && currentJob.equals("")) {
            for(Percept job: jobs.values()) {
                int endStep = getIntParam(job, 4);
                if((endStep - step) > 100) {
                    ParameterList items = (ParameterList) job.getParameters().get(5);
                    // TODO distribute items among team members
                }
            }
        }

        if(charge < .4 * battery) {
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
                        actionQueue.add(new Action("charge"));
                    }
                    else {
                        actionQueue.add(new Action("goto", 
                        new Numeral(chargingTarget.lat), new Numeral(chargingTarget.lon)));
                    }
                }
            }
        }

        /*
        if(leader.equals(getName())) {
            if(state == State.BUILD) {
                if(buildCounter-- == 0) {
                    state = State.EXPLORE;
                }
                else {
                    lastBuild = step;
                    actionQueue.add(new Action("build"));
                }
            }

            if(step - lastBuild > 30 && wellName != null && massium > wellCost) {
                state = State.BUILD;
                buildCounter = 20; // IMPROVE check actual progress
                actionQueue.add(new Action("build", new Identifier(wellName)));
            }
        }
        */

        if(exploreTarget != null) {
            if(atLoc(exploreTarget)) {
                // target reached
                exploreTarget = null;
            }
        }

        if(state == State.EXPLORE) {
            if(exploreTarget == null || lastActionResult.equalsIgnoreCase("failed_no_route")) {
                double expLat = minLat + rand.nextDouble() * (maxLat - minLat);
                double expLon = minLon + rand.nextDouble() * (maxLon - minLon);
                exploreTarget = new Loc(expLat, expLon);
            }
            actionQueue.add(new Action("goto", new Numeral(exploreTarget.lat), new Numeral(exploreTarget.lon)));
        }


		/*
		if(atLoc(shopLoc)){
		    String shopString = "";
            double minDist = Double.MAX_VALUE;
			if(numItems < 10) {
				for(Percept p: shopsPercept.values()) {
					double shopLat = getDoubleParam(p, 1);
					double shopLon = getDoubleParam(p, 2);
					double dist = Math.sqrt(Math.pow(lat - shopLat, 2) + Math.pow(lon - shopLon, 2));
					if(dist < minDist) {
						minDist = dist;
						shopString = getStringParam(p, 0);
					}
				}
			    Percept p = shopsPercept.get(shopString);
                shopLoc = new Loc(getDoubleParam(p, 1), getDoubleParam(p, 2));
				return new Action("goto", new Numeral(shopLoc.lat), new Numeral(shopLoc.lon));
			}
		}

		if(atLoc(shopLoc)){

		}
		*/

		// follow the plan if there is one
        if(actionQueue.size() > 0) return actionQueue.poll();

		// Associate a job to the agent
		if (myJob == null){
            Set<String> availableJobs = new HashSet<>(currentJobs.keySet());
            availableJobs.removeAll(jobsTaken);
            if(availableJobs.size() > 0){
                myJob = availableJobs.iterator().next();
                say("I will complete " + myJob);
                jobsTaken.add(myJob);
                broadcast(new Percept("taken", new Identifier(myJob)), getName());
            }
        }
		// If already associated a job, carry it out
		if(myJob != null){
            // plan the job
            // 1. acquire items
            Percept job = currentJobs.get(myJob);
            if(job == null){
                say("I lost my job :(");
                myJob = null;
                return new Action("skip");
            }
            String storage = stringParam(job.getParameters(), 1);
            ParameterList requirements = listParam(job, 4);
            for (Parameter requirement : requirements) {
                if(requirement instanceof Function){
                    // 1.1 get enough items of that type
                    String itemName = stringParam(((Function) requirement).getParameters(), 0);
                    int amount = intParam(((Function) requirement).getParameters(), 1);
                    if(itemName.equals("") || amount == -1){
                        say("Something is wrong with this item: " + itemName + " " + amount);
                        continue;
                    }
                    // find a shop selling the item
                    List<Percept> shops = shopsByItem.get(itemName);
                    if(shops.size() == 0){
                        say("I cannot buy the item " + itemName + "; this plan won't work very well.");
                    }
                    else{
                        say("I will go to the shop first.");
                        // go to the shop
                        Percept shop = shops.get(0);
                        actionQueue.add(new Action("goto", new Identifier(stringParam(shop.getParameters(), 0))));
                        // buy the items
                        actionQueue.add(new Action("buy", new Identifier(itemName), new Numeral(amount)));
                    }
                }
            }
            // 2. get items to storage
            actionQueue.add(new Action("goto", new Identifier(storage)));
			// 2.1 store items
			actionQueue.add(new Action("store"));
            // 2.2 deliver items
            actionQueue.add(new Action("deliver_job", new Identifier(myJob)));
        }

        /*
		if(numItems > 10) {
			String dumpString = "";
			double minDist = Double.MAX_VALUE;
			for(Percept p: dumpsPercept.values()) {
				double dumpLat = getDoubleParam(p, 1);
				double dumpLon = getDoubleParam(p, 2);
				double dist = Math.sqrt(Math.pow(lat - dumpLat, 2) + Math.pow(lon - dumpLon, 2));
				if(dist < minDist) {
					minDist = dist;
					dumpString = getStringParam(p, 0);
				}
			}
			Percept p = dumpsPercept.get(dumpString);
            dumpLoc = new Loc(getDoubleParam(p, 1), getDoubleParam(p, 2));
			return new Action("goto", new Numeral(dumpLoc.lat), new Numeral(dumpLoc.lon));
		}

		if(atLoc(dumpLoc)){
			return new Action("dump");
        }
        */

		return actionQueue.peek() != null? actionQueue.poll() : new Action("skip");
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
        EXPLORE, RECHARGE, JOB
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
