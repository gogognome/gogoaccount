package nl.gogognome.gogoaccount.component.importer;

import java.util.*;
import nl.gogognome.gogoaccount.test.builders.*;
import nl.gogognome.lib.text.*;
import nl.gogognome.lib.util.*;

public class TdbImportedTransaction {

	private String fromAccount = "NL12FROM0123456789";
	private String fromName = null;
	private Amount amount = AmountBuilder.build(123);
	private Date date = DateUtil.createDate(2023, 9, 7);
	private String toAccount = "NL12TOZZ9876543210";
	private String toName = "Piet Puk";
	private String description = "Payment of declaration 543";

	private TdbImportedTransaction() {
	}

	public static TdbImportedTransaction aNew() {
		return new TdbImportedTransaction();
	}

	public TdbImportedTransaction withToAccount(String toAccount) {
		this.toAccount = toAccount;
		return this;
	}

	public TdbImportedTransaction withDescription(String description) {
		this.description = description;
		return this;
	}

	public TdbImportedTransaction withToName(String toName) {
		this.toName = toName;
		return this;
	}

	public ImportedTransaction build() {
		return new ImportedTransaction(
				fromAccount,
				fromName,
				amount,
				date,
				toAccount,
				toName,
				description
		);
	}
}