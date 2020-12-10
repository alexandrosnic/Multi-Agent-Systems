package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;

import java.util.List;

/**
 * A very basic agent.
 */
public class AlexAgent extends Agent {

	private Map<String, Percept> jobs = new HashMap<>();
	private String role = "";
    private int battery = 0;


    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public AlexAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    @Override
    public void handlePercept(Percept percept) {}


    @Override
    public Action step() {
        List<Percept> percepts = getPercepts();
		for (Percept percept : percepts) {
            switch(percept.getName()) {
                case "job":
                    jobs.put(getStringParam(percept, 0), percept);
                    break;
                case "role":
                    role = getStringParam(percept, 0);
                    battery = getIntParam(percept, 9);
                    break;
        return new Action("skip");
    }

	private Action act() {
	}

	private int getIntParam(Percept percept, int position) {
        Parameter p = percept.getParameters().get(position);
        if (p instanceof Numeral) return ((Numeral) p).getValue().intValue();
        return 0;
    }

	private String getStringParam(Percept percept, int position) {
        Parameter p = percept.getParameters().get(position);
        if (p instanceof Identifier) return ((Identifier) p).getValue();
        return "";
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
            default:
                say("I cannot handle a message of type " + message.getName());
        }
    }
}
