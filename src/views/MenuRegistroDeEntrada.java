package views;

import java.util.Scanner;

public class MenuRegistroDeEntrada {
	
	private static Scanner print = new Scanner(System.in);

	public static void renderizar() {

		int op;

		do {
			System.out.println("\n".repeat(2));
			System.out.println("-- MENU REGISTRO DE ENTRADAS --");
			System.out.println("\n1. Cadastrar novo registro de entrada");
			System.out.println("2. Listar registros de entrada");
			System.out.println("3. Excluir registro de entrada");
			System.out.println("0. Voltar ao menu anterior");
			System.out.println("\nDigite a opção desejada: ");
			op = print.nextInt();

			switch (op) {
			case 1:
				RegistrarEntrada.renderizar();
				break;
			case 2:
				ListarEntradas.renderizar();		
				break;
			case 3:
				DeletarEntrada.renderizar();
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
