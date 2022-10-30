package views;

import java.util.Scanner;

public class Principal {
	
	private static Scanner print = new Scanner(System.in);

	public static void main(String[] args) {

		int opcao;

		do{
			System.out.println("\n".repeat(2));
			System.out.println("-- CONTROLE DE ESTOQUE -- ");
			System.out.println("\n1. Menu Produto");
			System.out.println("2. Menu Fornecedor");
			System.out.println("3. Menu Registro de Entradas");
			System.out.println("4. Menu Registro de Saídas");
			System.out.println("5. Visualizar Controle de estoque");
			System.out.println("0. Sair");
			System.out.println("\nDigite uma opção:");

			opcao = print.nextInt();

			switch(opcao){
			case 1:
				MenuProduto.renderizar();
				break;
			case 2: 
				MenuFornecedor.renderizar();
				break;
			case 3:
				MenuRegistroDeEntrada.renderizar();
				break;
			case 4:
				MenuRegistroDeSaida.renderizar();
				break;
			case 5:
				MostrarControleEstoque.renderizar();
				break;
			case 0:
				System.out.println("\nAplicação finalizada...");
				break;
			default:
				System.out.println("\nDigite uma opção válida");
				break;
			}

		} while (opcao != 0);
	}
}


