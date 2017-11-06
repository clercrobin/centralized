package template;

//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;


public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private int nbTasks;
    private long timeout_setup;
    private long timeout_plan;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long tic = System.currentTimeMillis();

        final double prob = 0.5;

        this.nbTasks = tasks.size();

        Vehicle[] vArray = new Vehicle[vehicles.size()];
        vArray = vehicles.toArray(vArray);

        Random r = new Random();

        MyPlan initialPlan = initialPlan(vArray, tasks); // Give all the tasks to the first vehicle

        MyPlan currentPlan = initialPlan;

        MyPlan finalPlan = currentPlan;

        currentPlan.print();

        int nbIterations = 0;
        while(nbIterations < 1000){
            System.out.println("Iteration " + (nbIterations + 1));


            List<MyPlan> possibleNeighbours = new ArrayList<MyPlan>();

            possibleNeighbours.addAll(interChanges(currentPlan));
            //System.out.println("Inter done");
            possibleNeighbours.addAll(intraChanges(currentPlan));
            //System.out.println("Intra done");



            MyPlan bestNeighbor = null;
            double minCost = Double.MAX_VALUE;


            for(MyPlan neighbor : possibleNeighbours) {
                if(neighbor.isValid(vArray, nbTasks)) {
                    double cost = neighbor.computePlanCost(vArray);
                    if (cost<minCost){
                        bestNeighbor = neighbor;
                        minCost = cost;
                    }
                }
            }

            double randomNumber = r.nextDouble();
            double currentCost = currentPlan.computePlanCost(vArray);

            if (minCost<currentCost || randomNumber>prob){
                currentPlan = bestNeighbor;
            }



            System.out.println("Cost [" + currentCost + "]");

            if(currentPlan.computePlanCost(vArray) < finalPlan.computePlanCost(vArray)){
                finalPlan = currentPlan;
            }

            randomNumber = r.nextDouble();

            if (randomNumber>0.99){
                currentPlan = finalPlan;
            }
            nbIterations++;
        }



        // Print the final possiblePlan.
        System.out.println("Costs : [" + finalPlan.computePlanCost(vArray) + "]");
        finalPlan.print();

        long toc = System.currentTimeMillis();
        long duration = toc - tic;
        System.out.println("Computing duration : " + duration + " milliseconds.");

        // Compare with the naive solution
        System.out.println("Naive cost : " + initialPlan.computePlanCost(vArray));

        List<Plan> plans = new LinkedList<Plan>();

        for (Vehicle vehicle : vehicles) {
            plans.add(finalPlan.generatePlan(vehicle));
        }

        return plans;
    }



    private MyPlan initialPlan(Vehicle[] vehicles, TaskSet tasks) {

        MyPlan s = new MyPlan(vehicles.length);

        for(Task t : tasks) {
            s.addTask(vehicles[0].id(), t);
        }

        return s;
    }

    private MyPlan naivePlan(Vehicle vehicle, TaskSet tasks) {
        MyPlan s = new MyPlan(1);
        int vehicleId = vehicle.id();

        for(Task task : tasks) {
            s.addTask(vehicleId, task);
            //s.addTask(vehicleId, delivery);
        }

        return s;
    }



    private Set<MyPlan> interChanges(MyPlan currentPlan) {
        //currentPlan.print();
        Random r = new Random();

        Set<MyPlan> neighbors = new HashSet<MyPlan>();

        int nbVehicles = currentPlan.getNbVehicles();
        //System.out.println(nbVehicles);

        for (int vehicleId = 0; vehicleId<currentPlan.getNbVehicles();vehicleId++){

        //int vehicleId = r.nextInt(currentPlan.getNbVehicles()-1);
            int nbTasksVehicle = currentPlan.getNbTasksVehicle(vehicleId);

            for (int i = 0; i < nbTasksVehicle-1; i++) { // For each of the vehicle tasks
                for (int recieverVehicle = 0; recieverVehicle < currentPlan.getNbVehicles(); recieverVehicle++) {
                    if(vehicleId != recieverVehicle) {
                        MyPlan neighbor = currentPlan.clone();
                        //neighbor.print();
                        neighbor = neighbor.interSwapTasks(vehicleId, recieverVehicle, i);
                        neighbors.addAll(intraChanges(neighbor));
                    }
                }
            }

        }


        return neighbors;
    }


    private Set<MyPlan> intraChanges(MyPlan currentPlan) {

        Random r = new Random();

        int i;
        int j;

        Set<MyPlan> neighbors = new HashSet<MyPlan>();

        int nbVehicles = currentPlan.getNbVehicles();

        for (int vehicleId = 0; vehicleId<currentPlan.getNbVehicles();vehicleId++) {

            int length = currentPlan.getNbTasksVehicle(vehicleId);


            if (length >= 2) {
                int switchNumber;
                switchNumber = r.nextInt(length);
                for (int sw = 0; sw < switchNumber; sw++) {
                    i = r.nextInt(length);
                    j = r.nextInt(length);
                    MyPlan neighbor = currentPlan.intraSwapTasks(vehicleId, i, j);
                    neighbors.add(neighbor);

                }

            }
        }

        return neighbors;
    }
}
