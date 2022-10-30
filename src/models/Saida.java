package models;

import java.util.Date;
import java.util.Random;

public class Saida {
	
    private Date criadoEm;
    private int ValorProduto;
    
    
     public Saida() {
        Random generator = new Random();
        setCriadoEm(new Date());
        setId(generator.nextInt(1000));
       
    }

	public Date getCriadoEm() {
		return criadoEm;
	}


	public void setCriadoEm(Date criadoEm) {
		this.criadoEm = criadoEm;
	}


	public int getValorProduto() {
		return ValorProduto;
	}


	public void setValorProduto(int valorProduto) {
		ValorProduto = valorProduto;
	}


}
