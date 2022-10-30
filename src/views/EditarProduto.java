package views;

import java.util.Scanner;

import controllers.ProdutoController;
import models.Produto;

public class EditarProduto {

	private static Scanner print = new Scanner(System.in);
	private static String codigoProduto;
	private static Produto produto;
	private static String novoCodigo = null;
	private static String novoNome = null;
	private static String novaCategoria = null;


	public static void renderizar(){
		
		produto = new Produto();

		System.out.println("\nDigite o código do produto");
		codigoProduto = print.next();
		produto = ProdutoController.buscarPorCodigo(codigoProduto);
		if (produto != null){

			int op;

			do {
				System.out.println("\nSelecione o item a ser editado:");
				System.out.println("1 - Código do produto");
				System.out.println("2 - Nome produto");
				System.out.println("3 - Categoria do produto");
				System.out.println("0 - Voltar");

				op = print.nextInt();

				switch(op){
				case 1:
					System.out.println("\nDigite o novo Código do Produto:");
					novoCodigo = print.next();
					System.out.println("\nCódigo do produto alterado com sucesso!");
					break;
				case 2: 
					System.out.println("\nDigite o novo Nome do Produto:");
					novoNome = print.next();
					System.out.println("\nNome do produto alterado com sucesso!");
					break;
				case 3:
					System.out.println("\nDigite a nova Categoria do Produto:");
					novaCategoria = print.next();
					System.out.println("\nCategoria do produto alterada com sucesso!");
					break;
				case 0:
					System.out.println("\nVoltando...");
					break;
				default:
					System.out.println("\nDigite uma opção válida\n");
				}

			} while (op != 0);

			ProdutoController.editar(codigoProduto, novoCodigo, novoNome, novaCategoria);

		} else{
			System.out.println("\nEste produto não existe!\n");
		}
	}
}
