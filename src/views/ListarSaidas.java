package views;

import controllers.SaidaController;
import models.Saida;

public class ListarSaidas {
	
	public static void renderizar(){
	    System.out.println("\n");     
	    System.out.println("-- Lista de saídas --");
	        
	    for(Saida saidaCadastrada: SaidaController.listar()){
	        System.out.println("\n");
	        System.out.println("Data: "+saidaCadastrada.getCriadoEm());
	        System.out.println("ID: "+saidaCadastrada.getId());
	        System.out.println("Produto: "+saidaCadastrada.getProduto().getNomeProduto());
	        System.out.println("Quantidade: "+saidaCadastrada.getQntdProduto());
	        System.out.println("Valor de Venda: "+saidaCadastrada.getValorProduto());
	        
	    }
	    
	    }

}
