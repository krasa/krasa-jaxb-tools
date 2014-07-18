package com.sun.tools.xjc.addon.krasa.validator;

/**
 * @author Vojtech Krasa
 */
@Choice({"tea", "coffee"})
public class DrinkType {
	protected String tea;
	protected String coffee;

	public String getTea() {
		return tea;
	}

	public void setTea(String tea) {
		this.tea = tea;
	}

	public String getCoffee() {
		return coffee;
	}

	public void setCoffee(String coffee) {
		this.coffee = coffee;
	}
}
