package template;

import java.util.*;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class MyPlan implements Cloneable {

	/* maps a vehicle id to a list of task ordered by order of action */

	private int nbVehicles;
	private HashMap<Integer, LinkedList<MyTask>> repartition;

	public MyPlan(int nbVehicles_) {
		this.nbVehicles = nbVehicles_;
		repartition = new HashMap<Integer, LinkedList<MyTask>>(nbVehicles_);
		for (int i = 0; i < nbVehicles_; i++) {
			repartition.put(i, new LinkedList<MyTask>()); // We initialize with
															// an empty linked
															// list
		}
	}

	public int getNbVehicles() {
		return nbVehicles;
	}

	@Override
	protected MyPlan clone() {
		MyPlan s = new MyPlan(nbVehicles);
		s.setPossiblePlan(new HashMap<Integer, List<MyTask>>(this.repartition));
		return s;
	}

	public void print() {
		for (Integer name : repartition.keySet()) {
			String key = name.toString();
			String value = repartition.get(name).toString();
			System.out.println("Vehicle " + key + " " + value);
		}
	}

	public void addTask(int vehicleId, Task task) {
		if (task == null)
			return;

		// Giving only one pickup or one delivery makes no sense, we would have
		// an invalid state
		MyTask pickup = new MyTask(task, true);
		MyTask delivery = new MyTask(task, false);

		this.repartition.get(vehicleId).addLast(pickup);
		this.repartition.get(vehicleId).addLast(delivery);
	}

	public void removeTask(int vehicleId, Task task) {
		Iterator<MyTask> vehicleTasks = this.repartition.get(vehicleId).iterator();
		if (task == null)
			return;

		int taskId = task.id;
		while (vehicleTasks.hasNext()) {
			MyTask iTask = vehicleTasks.next();
			if (iTask.getTask().id == taskId) {
				vehicleTasks.remove();
			}
		}
	}

	public boolean isValid(Vehicle[] vehicles, int nbTasks) {

		List<Integer> alreadyPickedUpTasks = new ArrayList<Integer>();
		List<Integer> alreadyDeliveredTasks = new ArrayList<Integer>();

		int totalTasks = 0;
		for (int i = 0; i < nbVehicles; i++) {
			int load = 0;

			List<MyTask> vehicleTasks = repartition.get(i);

			totalTasks += vehicleTasks.size();

			Set<Integer> carriedTasks = new HashSet<Integer>();
			for (MyTask task : vehicleTasks) {
				if (task.isPickup()) {
					load += task.getTask().weight;

					if (load > vehicles[i].capacity())
						return false;
					if (alreadyPickedUpTasks.contains(task.getTask().id))
						return false;
					alreadyPickedUpTasks.add(task.getTask().id);
					carriedTasks.add(task.getTask().id);

				} else {
					if (alreadyDeliveredTasks.contains(task.getTask().id))
						return false;
					if (!carriedTasks.contains(task.getTask().id))
						return false;

					alreadyDeliveredTasks.add(task.getTask().id);

					load -= task.getTask().weight;

				}
			}
		}

		if (totalTasks / 2 != nbTasks)
			return false;

		return true;
	}

	public LinkedList<MyTask> getVehicleTasks(int vehicleId) {
		if (repartition.containsKey(vehicleId)) {
			if (repartition.get(vehicleId).isEmpty())
				return null;
			else
				return repartition.get(vehicleId);
		} else
			return null;
	}

	public int getNbTasksVehicle(int vehicleId) {
		return repartition.get(vehicleId).size();
	}

	public MyPlan interSwapTasks(int vehicleId1, int vehicleId2, int taskIndex) {

		MyTask tv1 = getVehicleTasks(vehicleId1).get(taskIndex);

		if (tv1 != null && vehicleId1 < this.nbVehicles && vehicleId2 < this.nbVehicles) {
			this.removeTask(vehicleId1, tv1.getTask());
			this.addTask(vehicleId2, tv1.getTask());
		}

		return this;
	}

	public MyPlan intraSwapTasks(int vehicleId, int taskIndex1, int taskIndex2) {
		MyPlan neighbor = this.clone();

		LinkedList<MyTask> currentVehicleTasks = this.repartition.get(vehicleId);
		LinkedList<MyTask> newVehicleTasks = new LinkedList<MyTask>();

		for (int i = 0; i < currentVehicleTasks.size(); i++) {
			if (i == taskIndex1)
				newVehicleTasks.add(currentVehicleTasks.get(taskIndex2));
			else if (i == taskIndex2)
				newVehicleTasks.add(currentVehicleTasks.get(taskIndex1));
			else
				newVehicleTasks.add(currentVehicleTasks.get(i));
		}

		neighbor.repartition.put(vehicleId, newVehicleTasks);

		return neighbor;
	}

	public double computePlanCost(Vehicle[] vehicles) {
		double totalCost = 0.0;

		for (int i = 0; i < vehicles.length; i++) {
			List<MyTask> tasks = repartition.get(i);

			City tmpCity = vehicles[i].getCurrentCity();

			for (MyTask t : tasks) {
				City nextCity = t.isPickup() ? t.getTask().pickupCity : t.getTask().deliveryCity;
				totalCost += (tmpCity.distanceTo(nextCity) * vehicles[i].costPerKm());
				tmpCity = nextCity;
			}
		}

		return totalCost;
	}

	public Plan generatePlan(Vehicle vehicle) {
		int vehicleId = vehicle.id();
		City current = vehicle.getCurrentCity();

		Plan p = new Plan(current);

		List<MyTask> tasks = repartition.get(vehicleId);

		City intermediateCity = current;
		for (MyTask t : tasks) {
			City nextDestination = t.isPickup() ? t.getTask().pickupCity : t.getTask().deliveryCity;

			for (City c : intermediateCity.pathTo(nextDestination))
				p.appendMove(c);

			if (t.isPickup())
				p.appendPickup(t.getTask());
			else
				p.appendDelivery(t.getTask());

			intermediateCity = nextDestination;
		}

		return p;
	}

	public void setPossiblePlan(HashMap<Integer, List<MyTask>> solution) {

		this.repartition.clear();

		for (Map.Entry<Integer, List<MyTask>> entry : solution.entrySet()) {
			LinkedList<MyTask> tasks = new LinkedList<MyTask>();

			for (MyTask extendedTask : entry.getValue())
				tasks.add(extendedTask);

			this.repartition.put(entry.getKey(), tasks);
		}
	}

	public boolean isEquals(MyPlan s) {
		return true;
	}

}
