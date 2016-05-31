package com.unifun.sigtran.smsgate.hibernate.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="substitution_sourche_address")
public class SourcheSubstitutionList {

	@Id
	@Column(name="sourche_original", nullable=false)
	private String sourcheOriginal;
	@Column(name="sourche_substitution", nullable=false)
	private String sourcheSubstitution;
	
	 public SourcheSubstitutionList() {
		// TODO Auto-generated constructor stub
	}

	public String getSourcheOriginal() {
		return sourcheOriginal;
	}

	public void setSourcheOriginal(String sourcheOriginal) {
		this.sourcheOriginal = sourcheOriginal;
	}

	public String getSourcheSubstitution() {
		return sourcheSubstitution;
	}

	public void setSourcheSubstitution(String sourcheSubstitution) {
		this.sourcheSubstitution = sourcheSubstitution;
	}
	 
	 
}
