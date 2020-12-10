package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;

import java.util.List;

/**
 * A very basic agent.
 */
public class AlexAgent extends Agent {

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
    public void handleMessage(Percept message, String sender) {}

    @Override
    public Action step() {
        List<Percept> percepts = getPercepts();
		for (Percept percept : percepts) {
            switch(percept.getName()) {
                case "job":
                    jobs.put(getStringParam(percept, 0), percept);
                    break;
                case "resourceNode":

        return new Action("skip");
    }
}
