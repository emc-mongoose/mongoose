package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
//
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 23.10.14.
 */
@Entity(name = "MessageClassEntity")
@Table(name = "classes", uniqueConstraints = {
		@UniqueConstraint(columnNames = "name")})
public class MessageClassEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=MessageEntity.class, fetch = FetchType.LAZY, mappedBy = "classMessage")
	private Set<MessageEntity> messageSet = new HashSet<MessageEntity>();
	//
	public MessageClassEntity(){
	}
	public MessageClassEntity(final String name){
		this.name = name;
	}
	//
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public BigInteger getId() {
		return id;
	}
	public void setId(final BigInteger id) {
		this.id = id;
	}
	public Set<MessageEntity> getMessageSet() {
		return messageSet;
	}
	public void setMessageSet(final Set<MessageEntity> messageSet) {
		this.messageSet = messageSet;
	}
}
