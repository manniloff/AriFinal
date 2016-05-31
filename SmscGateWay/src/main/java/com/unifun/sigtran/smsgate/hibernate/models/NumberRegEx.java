package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="number_reg_ex")
public class NumberRegEx {

	public NumberRegEx() {
		// TODO Auto-generated constructor stub
	}
	
	@Id
	@GeneratedValue
	private int id;
	
	@Column(name = "ton", nullable = false, length=2)
	private String ton;
	
	@Column(name = "np", nullable = false, length=2)
	private String np;
	
	@Column(name = "expression", nullable = false, length=20)
	private String expression;

	@Column(name = "check_for_source_add", nullable = false)
	private boolean checkForSourceAdd;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTon() {
		return ton;
	}

	public void setTon(String ton) {
		this.ton = ton;
	}

	public String getNp() {
		return np;
	}

	public void setNp(String np) {
		this.np = np;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public boolean isCheckForSourceAdd() {
		return checkForSourceAdd;
	}

	public void setCheckForSourceAdd(boolean checkSourceAdd) {
		this.checkForSourceAdd = checkSourceAdd;
	}
	
}
