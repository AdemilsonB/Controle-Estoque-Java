package models;

import java.util.Date;
import java.util.Random;


public class Entrada extends Fluxo {
	
    private Date dataCompra;
    private double custoUnidade;

    public Entrada() {
        Random generator = new Random();
        setDataCompra(new Date());
       setId(generator.nextInt(1000));  
       
    }

	public Date getDataCompra() {
		return dataCompra;
	}

	public void setDataCompra(Date dataCompra) {
		this.dataCompra = dataCompra;
	}

	public double getCustoUnidade() {
		return custoUnidade;
	}

	public void setCustoUnidade(double custoUnidade) {
		this.custoUnidade = custoUnidade;
	}

	
	
}
