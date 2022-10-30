package views;

import java.util.Scanner;

import controllers.ProdutoController;
import models.Produto;

public class CadastrarProduto {

	 private static Produto produto;
	 private static Scanner print = new Scanner(System.in);
	    
	 public static void renderizar() {
		 
	    produto = new Produto();
	         
	    System.out.println("\n");
	    System.out.println("\n-- CADASTRAR PRODUTO --  \n");
	    System.out.println("Digite o código do produto:");
	    String codigoProduto = print.next();
	    if (ProdutoController.buscarPorCodigo(codigoProduto) == null) {
	    	produto.setCodigoProduto(codigoProduto);
	    	System.out.println("Digite o nome do produto:");
		    produto.setNomeProduto(print.next());
		    System.out.println("Digite a categoria do produto:");
		    produto.setCategoriaProduto(print.next());
		         
		    ProdutoController.cadastrar(produto);
		    System.out.println("\nProduto cadastrado com sucesso!");
	         
	    }else {
	        System.out.println("\nUm produto com este código já existe!");
	    }
	}
}

	    

