package views;

import java.util.Scanner;

import controllers.ProdutoController;
import controllers.SaidaController;
import models.Produto;
import models.Saida;


public class RegistrarSaida {
	
	private static Scanner print = new Scanner(System.in);
    private static Saida saida;
    private static String codigoProduto;
    private static Produto produto;
    private static int quantidade, quantidadeEstoque;
    
    
    
      public static void renderizar(){
        produto = new Produto();
        saida = new Saida();
        
        System.out.println("\n");
        System.out.println("\nDigite o código do produto:");
        codigoProduto = print.next();
        produto = ProdutoController.buscarPorCodigo(codigoProduto);
        if (produto != null){
            saida.setProduto(produto);
        
            System.out.println("\n Digite a quantidade de produto(s):");
            quantidade = print.nextInt();
            quantidadeEstoque = produto.getQntdEstoque();
            if (quantidade > quantidadeEstoque){
                System.out.println("o estoque possui apenas "+quantidadeEstoque+" itens deste produto!" );
            } else {
                saida.setQntdProduto(quantidade);

                System.out.println("\nDigite o valor do produto:");
                saida.setValorProduto(print.nextInt());
                    
                if(SaidaController.registrar(saida)) {
                    System.out.println("Saida registrada com sucesso!");
                }
            }
     
        } else{
            System.out.println("Esse produto não foi encontrado");
        }
    }

}
