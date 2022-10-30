package views;

import java.util.Scanner;

public class MenuProduto {
	
	private static Scanner print = new Scanner(System.in);

	public static void renderizar() {

		int op;

		do {
			System.out.println("\n".repeat(2));
			System.out.println("-- MENU PRODUTO --");
			System.out.println("\n1. Cadastrar produto");
			System.out.println("2. Listar produtos");
			System.out.println("3. Alterar produto");
			System.out.println("4. Deletar produto");
			System.out.println("0. Voltar ao menu anterior");
			System.out.println("\nDigite a opção desejada: ");
			op = print.nextInt();

			switch (op) {
			case 1:
				CadastrarProduto.renderizar();
				break;
			case 2:
				ListarProdutos.renderizar();		
				break;
			case 3:
				EditarProduto.renderizar();
				break;
			case 4:
				DeletarProduto.renderizar();
				break;
			case 0:
				System.out.println("\nVoltando...");
				break;
			default:
				System.out.println("\nOpção inválida!");
				break;
			}

		} while (op != 0);
	}
}