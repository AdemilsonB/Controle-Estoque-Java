package views;

import java.util.Scanner;

import controllers.FornecedorController;
import models.Fornecedor;

public class EditarFornecedor {

	private static Scanner print = new Scanner(System.in);
	private static String cnpj;
	private static Fornecedor fornecedor;
	private static String novoCnpj = null;
	private static String novoNome = null;
	private static String novoTelefone = null;
	private static String novoEmail = null;
	private static String novoEndereco = null;


	public static void renderizar(){
		
		fornecedor = new Fornecedor();

		System.out.println("\nDigite o cnpj do fornecedor");
		cnpj = print.next();
		fornecedor = FornecedorController.buscarPorCnpj(cnpj);
		if (fornecedor != null){
			
			int op;
			
			do {
				System.out.println("\nSelecione o item a ser editado:");
				System.out.println("1 - CNPJ do fornecedor");
				System.out.println("2 - Nome do fornecedor");
				System.out.println("3 - Telefone do fornecedor");
				System.out.println("4 - E-mail do fornecedor");
				System.out.println("5 - Endereço do fornecedor");
				System.out.println("0 - Voltar");

				op = print.nextInt();

				switch(op){
				case 1:
					System.out.println("Digite o novo CNPJ do fornecedor:");
					novoCnpj = print.next();
					System.out.println("\nCNPJ do fornecedor alterado com sucesso!");
					break;
				case 2: 
					System.out.println("Digite o novo Nome do fornecedor:");
					novoNome = print.next();
					System.out.println("\nNome do fornecedor alterado com sucesso!");
					break;
				case 3:
					System.out.println("Digite o novo telefone do fornecedor:");
					novoTelefone = print.next();
					System.out.println("\nTelefone do fornecedor alterado com sucesso!");
					break;
				case 4:
					System.out.println("Digite o novo e-mail do fornecedor:");
					novoEmail = print.next();
					System.out.println("\nEmail do fornecedor alterado com sucesso!");
					break;
				case 5:
					System.out.println("Digite o novo endereço do fornecedor:");
					novoEndereco = print.next();
					System.out.println("\nEndereço do fornecedor alterado com sucesso!");
					break;
				case 0:
					System.out.println("Voltando...");
					break;
				default:
					System.out.println("\nDigite uma opção válida\n");
				}
				
			} while (op != 0);
			
			FornecedorController.editar(cnpj, novoCnpj, novoNome, novoTelefone, novoEmail, novoEndereco);
			
		} else{
			System.out.println("\nEste fornecedor não existe!\n");
		}
	}

}
