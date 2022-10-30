package views;

import java.util.Scanner;

public class MenuFornecedor {
	
	private static Scanner print = new Scanner(System.in);

	public static void renderizar() {

		int op;

		do {
			System.out.println("\n".repeat(2));
			System.out.println("-- MENU FORNECEDOR --");
			System.out.println("\n1. Cadastrar fornecedor");
			System.out.println("2. Listar fornecedores");
			System.out.println("3. Alterar fornecedor");
			System.out.println("4. Deletar fornecedor");
			System.out.println("0. Voltar ao menu anterior");
			System.out.println("\nDigite a opção desejada: ");
			op = print.nextInt();

			switch (op) {
			case 1:
				CadastrarFornecedor.renderizar();
				break;
			case 2:
				ListarFornecedores.renderizar();		
				break;
			case 3:
				EditarFornecedor.renderizar();
				break;
			case 4:
				DeletarFornecedor.renderizar();
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