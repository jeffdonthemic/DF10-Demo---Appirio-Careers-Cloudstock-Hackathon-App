package com.appirio.jobs.domain;

public class Job {
	
	public Job(String id, String name, String jobTitle, String location, 
			String duties, String skills, String salary, String boxUrl) {
		
		this.id = id;
		this.name = name;
		this.jobTitle = jobTitle;
		this.location = location;
		this.duties = duties;
		this.skills = skills;
		this.salary = salary;
		this.boxUrl = boxUrl;
		
	}

	private String id;
	private String name;
	private String jobTitle;
	private String duties;
	private String location;
	private String skills;
	private String salary;
	private String boxUrl;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getJobTitle() {
		return jobTitle;
	}
	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}
	public String getDuties() {
		return duties;
	}
	public void setDuties(String duties) {
		this.duties = duties;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getSkills() {
		return skills;
	}
	public void setSkills(String skills) {
		this.skills = skills;
	}
	public String getSalary() {
		return salary;
	}
	public void setSalary(String salary) {
		this.salary = salary;
	}
	public String getBoxUrl() {
		return boxUrl;
	}
	public void setBoxUrl(String boxUrl) {
		this.boxUrl = boxUrl;
	}
	
}
