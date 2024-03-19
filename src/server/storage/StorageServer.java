package server.storage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import model.Car;
import model.EconomicCar;
import model.ExecutiveCar;
import model.IntermediaryCar;

public class StorageServer implements StorageInterface {

	private String path = "src/server/storage/cars.txt";
	private static HashMap<String, Car> cars;
	private static ObjectOutputStream fileOutput;
	private static ObjectInputStream fileInput;
	private static int economicAmount = 0, intermediaryAmount = 0, executiveAmount = 0; 
	
	public StorageServer() {
		try { // tenta abrir
			fileInput = new ObjectInputStream(new FileInputStream(path));
		} catch (IOException e) { // se nao abrir pq nao existe/funciona
			
			try { // ele faz o output pra poder criar o arquivo certo e dps abre
				fileOutput = new ObjectOutputStream(new FileOutputStream(path));
				fileInput = new ObjectInputStream(new FileInputStream(path));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		cars = getFileCars();
	}
	
	public static void main(String[] args) {
		
		StorageServer storServer = new StorageServer();

		try {
			StorageInterface server = (StorageInterface) UnicastRemoteObject.exportObject(storServer, 0);

			LocateRegistry.createRegistry(5002);
			Registry register = LocateRegistry.getRegistry("127.0.0.2", 5002);
			register.bind("Storage", server);

			System.out.println("Servidor de Armazenamento ligado.");

		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void addCar(Car newCar) {
		cars = getFileCars(); // pega do arquivo e bota no mapa
		cars.put(newCar.getRenavam(), newCar); // add no mapa
		attServer(); // salva o mapa num arquivo
		
		System.out.println("Carro adicionado com sucesso.");
	}

	@Override
	public void editCar(String renavam, Car editedCar) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		Car editCar = searchCar(renavam);
		
		if(editedCar.getName() != null) {
			editCar.setName(editedCar.getName());
		}
		if(editedCar.getCategory() != 0) {
			switch(editCar.getCategory()) {
			case 1:
				economicAmount--;
				break;
			case 2:
				intermediaryAmount--;
				break;
			case 3:
				executiveAmount--;
				break;
			}
			switch(editedCar.getCategory()) {
			case 1:
				economicAmount++;
				break;
			case 2:
				intermediaryAmount++;
				break;
			case 3:
				executiveAmount++;
				break;
			}
			editCar.setCategory(editedCar.getCategory());
		}
		if(editedCar.getManufactureYear() != null) {
			editCar.setManufactureYear(editedCar.getManufactureYear());
		}
		if(editedCar.getPrice() != 0.0) {
			editCar.setPrice(editedCar.getPrice());
		}
		
		System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
		
		attServer(); // salva o mapa num arquivo
	}

	@Override
	public void deleteCar(String renavam) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		Car deleteCar = searchCar(renavam);
		
		if(deleteCar != null) {
			cars.remove(renavam, deleteCar);
			System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
		}
		
		attServer(); // salva o mapa num arquivo
	}
	
	@Override
	public void deleteCars(String name) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> deleteCars = searchCars(name);
		
		if(deleteCars != null) {
			for(Car toDeleteCar : deleteCars) {
				cars.remove(toDeleteCar.getRenavam(), toDeleteCar);
			}
			System.out.println("Todos os carros " + name + " deletados com sucesso.");
		}
		
		attServer(); // salva o mapa num arquivo
	}

	@Override
	public List<Car> listCars() {
		cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> list = new ArrayList<Car>();
		
		for (Entry<String, Car> car : cars.entrySet()) {
			list.add(car.getValue());
		}
		
		// processo de ordenar por nome
		Comparator<Car> comparator = Comparator.comparing(Car::getName);
		Collections.sort(list, comparator);
		
		System.out.println("Lista de carros enviada.");
		
		return list;
	}
	
	@Override
	public List<Car> listCars(int category) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> list = new ArrayList<Car>();
		
		for (Entry<String, Car> car : cars.entrySet()) {
			if(car.getValue().getCategory() == category) {
				list.add(car.getValue());	
			}
		}
		
		// processo de ordenar por nome
		Comparator<Car> comparator = Comparator.comparing(Car::getName);
		Collections.sort(list, comparator);
				
		System.out.println("Lista de carros da categoria " + category + " enviada.");
		
		return list;
	}

	@Override
	public Car searchCar(String renavam) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		
		Car finded = null;
		for (Entry<String, Car> car : cars.entrySet()) {
			if (renavam.equals(car.getKey()) && renavam.equals(car.getValue().getRenavam())) {
				System.out.println("Carro encontrado com sucesso! Nome: " + car.getValue().getName() + ".");
				finded = car.getValue();
				break;
			}
		}
		
		return finded;

	}

	@Override
	public List<Car> searchCars(String name) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		
		List<Car> findeds = new ArrayList<Car>();
		for (Entry<String, Car> car : cars.entrySet()) {
			if (name.equalsIgnoreCase(car.getValue().getName())) {
				System.out.println("Renavam: " + car.getValue().getRenavam() + ".");
				findeds.add(car.getValue());
			}
		}
		
		return findeds;
	}

	@Override
	public Car buyCar(String renavam) {
		
		Car purchased = searchCar(renavam);
		System.out.println("Carro de renavam " + renavam + " foi comprado.");
		deleteCar(renavam);
		
		return purchased;
	}
	
	private static HashMap<String, Car> getFileCars() {
		boolean eof = false;
		
		if(cars == null) {
			cars = new HashMap<String, Car>();
		}
		
		try {
			while (!eof) {
				Car registredCar = (Car) fileInput.readObject();
				cars.put(registredCar.getRenavam(), registredCar);
			}

		} catch (IOException e) {
			eof = true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return cars;
	}
	
	private void attServer() {

		try {
			fileOutput = new ObjectOutputStream(new FileOutputStream(path));
			
			economicAmount = 0;
			intermediaryAmount = 0;
			executiveAmount = 0;
			for (Entry<String, Car> car : cars.entrySet()) {
				fileOutput.writeObject(car.getValue());
				
				switch(car.getValue().getCategory()) {
				case 1:
					economicAmount++;
					break;
				case 2:
					intermediaryAmount++;
					break;
				case 3:
					executiveAmount++;
					break;
				default:
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public int getAmount(int category) throws RemoteException {
		cars = getFileCars();
		attServer();
		
		switch(category) {
		case 1:
			EconomicCar.setAmount(economicAmount);
			return economicAmount;
		case 2:
			IntermediaryCar.setAmount(intermediaryAmount);
			return intermediaryAmount;
		case 3:
			ExecutiveCar.setAmount(executiveAmount);
			return executiveAmount;
		default:
			EconomicCar.setAmount(economicAmount);
			IntermediaryCar.setAmount(intermediaryAmount);
			ExecutiveCar.setAmount(executiveAmount);
			return economicAmount + intermediaryAmount + executiveAmount;	
		}
	}
	
}
