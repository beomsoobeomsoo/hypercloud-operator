package k8s.example.client.metering.models;

public class Metering {
	String namespace;
	double cpu = 0;
	long memory = 0;
	long storage = 0;
	int publicIp = 0;
	int privateIp = 0;
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public double getCpu() {
		return cpu;
	}
	public void setCpu(double cpu) {
		this.cpu = cpu;
	}
	public long getMemory() {
		return memory;
	}
	public void setMemory(long memory) {
		this.memory = memory;
	}
	public long getStorage() {
		return storage;
	}
	public void setStorage(long storage) {
		this.storage = storage;
	}
	public int getPublicIp() {
		return publicIp;
	}
	public void setPublicIp(int publicIp) {
		this.publicIp = publicIp;
	}
	public int getPrivateIp() {
		return privateIp;
	}
	public void setPrivateIp(int privateIp) {
		this.privateIp = privateIp;
	}
}